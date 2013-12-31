package org.deepdive

import akka.actor._
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import org.deepdive.profiling._
import scala.collection.mutable.{Set, Map}
import scala.concurrent.duration._
import scala.util.Try

object TaskManager {
  
  def props = Props(classOf[TaskManager])

  case class AddTask(task: Task)
  case class Done(task: Task, result: Try[_])
  case class Subscribe(taskId: String)
  case class Unsubscribe(taskId: String)

}

class TaskManager extends Actor with ActorLogging {

  import TaskManager._

  implicit val taskTimeout = Timeout(24 hours)
  import context.dispatcher

  val taskQueue = Set[Task]()
  val runningTasks = Set[Task]()
  val completedTasks = Set[Done]()
  val subscribers = Map[String, List[ActorRef]]()

  override def preStart() {
    log.info(s"starting at ${self.path}")
  }

  def receive = {
    case AddTask(task) =>
      taskQueue += task
      log.info(s"Added task_id=${task.id}")
      // Subscribe the sender
      // self.tell(Subscribe(task.id), sender)
      scheduleTasks()
    
    case msg @ Done(task, result) =>
      val reportDesc = if (result.isSuccess) "SUCCESS" else "FAILURE"
      context.system.eventStream.publish(EndReport(task.id, Option(reportDesc)))
      runningTasks -= task
      completedTasks += msg
      log.info(s"Completed task_id=${task.id}")
      // Notify the subscribers
      subscribers.get(task.id).getOrElse(Nil).foreach(_ ! result)
      scheduleTasks()
    
    case Subscribe(taskId) =>
      val currentSubscribers = subscribers.get(taskId).getOrElse(Nil)
      subscribers += Tuple2(taskId, (currentSubscribers :+ sender))
      log.info(s"Subscribed actorRef=${sender} to task_id=${taskId}")
      // If the task is already completed, notify the sender immediately
      val completedTask = completedTasks.find(_.task.id == taskId)
      completedTask.foreach ( task => sender ! completedTask)
    
    case Unsubscribe(taskId) =>
      val currentSubscribers = subscribers.get(taskId).getOrElse(Nil)
      val newSubscribers = currentSubscribers.filterNot(_ == sender)
      subscribers +=  Tuple2(taskId, newSubscribers)
      log.info(s"Unsubscribed actorRef=${sender} from task_id=${taskId}")

    case "shutdown" =>
      context.system.shutdown()

    case msg =>
      log.warning(s"Huh? ${msg}")

  }

  // Forwards eligible task to the responsible actor
  def scheduleTasks() = {
    // Find task that have all dependencies satisfied
    val (eligibileTasks, notEligibleTasks) = taskQueue.partition { task =>
      task.dependencies.toSet.subsetOf(completedTasks.map(_.task.id))
    }

    log.info(s"${eligibileTasks.size}/${taskQueue.size} tasks eligible")
    log.info(s"not_eligible=${notEligibleTasks.map(_.id).toSet}")
    
    // Forward eligible tasks
    eligibileTasks.foreach { task =>
      log.debug(s"Sending task_id=${task.id} to ${task.worker}")
      context.system.eventStream.publish(StartReport(task.id, task.id))
      (task.worker ? task.taskDescription).onComplete ( x => self ! Done(task, x) )
      taskQueue -= task
      runningTasks += task
    }
  }

}
