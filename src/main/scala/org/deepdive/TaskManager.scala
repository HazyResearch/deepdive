package org.deepdive

import akka.actor._
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import org.deepdive.profiling._
import scala.collection.mutable.{Set, Map}
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Try, Failure, Success}

object TaskManager {
  
  def props = Props(classOf[TaskManager])

  case class AddTask(task: Task)
  case class Done(task: Task, result: Try[_])
  case object ForceShutdown
  case object Shutdown
  case object PrintStatus

}

class TaskManager extends Actor with ActorLogging {

  import TaskManager._

  implicit val taskTimeout = Timeout(200 hours)
  import context.dispatcher

  val taskQueue = Set[Task]()
  val runningTasks = Set[Task]()
  val resultsQueue = Map[Task, Future[_]]()
  val completedTasks = Set[Done]()
  val subscribers = Map[String, List[ActorRef]]()

  override def preStart() {
    log.info(s"starting at ${self.path}")
    // Periodically print the status
    context.system.scheduler.schedule(60.seconds, 60.seconds, self, PrintStatus)
  }

  def receive = {
    case AddTask(task) =>
      taskQueue += task
      // Watch the worker, fail the task if it crashes
      if (task.worker != self) {
        context.watch(task.worker)
      }
      log.info(s"Added task_id=${task.id}")
      // Subscribe the sender
      // self.tell(Subscribe(task.id), sender)

      // Don't print task pool status at initialization
      scheduleTasks(false)
    
    case msg @ Done(task, result) =>
      val reportDesc = result match {
        case Success(x) => "SUCCESS"
        case Failure(ex) => "FAILURE"
      }
      context.system.eventStream.publish(EndReport(task.id, Option(reportDesc)))
      runningTasks -= task
      completedTasks += msg
      log.info(s"Completed task_id=${task.id} with ${result.toString}")
      result match {
        case Success(x) => 
          scheduleTasks()
        case Failure(exception) => 
          log.error(s"task=${task.id} Failed: ${exception}")
          self ! ForceShutdown
      }        

    case x : Terminated =>
      val worker = x.actor
      log.info(s"$worker was terminated. Canceling its tasks.")
      val workerTasks = runningTasks.find(_.worker == worker)
      workerTasks.foreach { task =>
        self ! Done(task, Failure(new RuntimeException("worker was terminated")))
      }
      context.unwatch(worker)

    case ForceShutdown =>
      log.error("Forcing shutdown")
      // Stop all running tasks
      for (task <- runningTasks) {
        log.error(s"Stopping task=${task.id}")
        context.unwatch(task.worker)
        self ! Done(task, Failure(new RuntimeException("Task stopped")))
      }
      // Clear all cancellable outstanding tasks
      for (task <- taskQueue if task.cancellable) {
        log.error(s"Cancelling task=${task.id}")
        taskQueue -=task
        completedTasks += Done(task, Failure(new RuntimeException("Task cancelled")))
      }
      import scala.sys.process
      import scala.sys.process._
      import java.lang.management
      import sun.management.VMManagement;
      import java.lang.management.ManagementFactory;
      import java.lang.management.RuntimeMXBean;
      import java.lang.reflect.Field;
      import java.lang.reflect.Method;
      var pid = ManagementFactory.getRuntimeMXBean().getName().toString
      val pattern = """\d+""".r
      pattern.findAllIn(pid).foreach(id => (s"kill -9 ${id}").!!)

      self ! Shutdown
      scheduleTasks()

    case Shutdown =>
      context.stop(self)
      context.system.shutdown()

    case PrintStatus =>
      val runtime = Runtime.getRuntime
      val usedMemory = (runtime.totalMemory - runtime.freeMemory) / (1024 * 1024)
      val freeMemory = runtime.freeMemory / (1024 * 1024)
      val totalMemory = runtime.totalMemory / (1024 * 1024)
      val maxMemory = runtime.maxMemory / (1024 * 1024)
      log.info(s"Memory usage: ${usedMemory}/${totalMemory}MB (max: ${maxMemory}MB)")

    case msg =>
      log.warning(s"Huh? ${msg}")

  }

  // Forwards eligible task to the responsible actor
  def scheduleTasks(printing: Boolean = true) = {
    // Find task that have all dependencies satisfied
    val (eligibileTasks, notEligibleTasks) = taskQueue.partition { task =>
      task.dependencies.toSet.subsetOf(completedTasks.map(_.task.id))
    }

    printing match {
      case true => log.debug(s"${eligibileTasks.size}/${taskQueue.size} tasks eligible. Waiting tasks: ${notEligibleTasks.map(_.id).toSet}")
      case _ =>
    }
    
    // Forward eligible tasks
    eligibileTasks.foreach { task =>
      log.debug(s"Sending task_id=${task.id} to ${task.worker}")
      context.system.eventStream.publish(StartReport(task.id, task.id))
      val result = task.worker ? task.taskDescription
      result.onComplete ( x => self ! Done(task, x) )
      taskQueue -= task
      runningTasks += task
    }
  }

}
