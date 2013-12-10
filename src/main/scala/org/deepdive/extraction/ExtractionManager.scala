package org.deepdive.extraction

import akka.actor.SupervisorStrategy._
import akka.actor.{Actor, ActorRef, Props, ActorLogging, FSM, OneForOneStrategy}
import org.deepdive.context._
import org.deepdive.settings._
import org.deepdive.extraction._
import scala.collection.mutable.{PriorityQueue, ArrayBuffer, Map}
import scala.concurrent.duration._
import scala.util.Try

object ExtractionManager {
  def props : Props = Props(classOf[ExtractionManager])
  // Messages
  sealed trait Message
  case class AddTask(task: ExtractionTask) extends Message
  case class TaskCompleted(task: ExtractionTask) extends Message
}

/* 
 * Manages extraction tasks. The ExtractionManager is responsible for executing
 * extractions tasks in the correct order. It also parallelizes execution when possible.
 */ 
class ExtractionManager extends Actor with ActorLogging {
  import ExtractionManager._
    
  val PARALLELISM = 1
  val RETRY_TIMEOUT = 5.seconds

  // Keeps track of the tasks
  val taskQueue = PriorityQueue[ExtractionTask]()(ExtractionTaskOrdering.reverse)
  val runningTasks = Map[ExtractionTask, ActorRef]()
  val completedTasks = ArrayBuffer[ExtractionTask]()
  // Who wants to be notified when a task is done?
  val taskListeners = Map[ExtractionTask, ActorRef]()

  override def preStart(){
    log.debug("Starting")
  }

  /* When a failure happens we stop the executor and requeue the task */
  override val supervisorStrategy = OneForOneStrategy() {
    case _: Exception =>
      runningTasks.find(_._2 == sender).foreach { case(task, executor) =>
        runningTasks -= task
        log.error(s"Failed to execute task=$task.name. Retrying.")
        import context.dispatcher
        context.system.scheduler.scheduleOnce(RETRY_TIMEOUT, self, AddTask(task))
      }
      Stop
    }

  def receive = {
    case AddTask(task) =>
      log.debug(s"Adding task_name=${task.name}")
      taskQueue += task
      taskListeners += Tuple2(task, sender)
      scheduleTasks
    case TaskCompleted(task) =>
      log.debug(s"Completed task_name=${task.name}")
      runningTasks -= task
      completedTasks += task
      // Notify the listener
      taskListeners.get(task).foreach(_ ! TaskCompleted(task))
      scheduleTasks
    case msg => log.warning(s"Huh? ($msg)")
  }

  // Schedules new taks based on the queue and capacity
  private def scheduleTasks() : Unit = {
    log.debug("scheduling tasks")
    // How many more tasks can we execute in parallel right now?
    val capacity = PARALLELISM - runningTasks.size
    // Get the tasks from the queue, but only take those that have all dependencies satisfied
    val completedRelations = completedTasks.map(_.outputRelation).toSet

    // A bit ugly, but unfortunately Scala's Queue doesn't provide functional abstractions
    val (eligibleTasks, notEligibleTasks) = (1 to capacity).map { i =>
      Try(taskQueue.dequeue)
    }.flatMap(_.toOption).partition { task =>
      val dependencies = Context.settings.findExtractorDependencies(task.name)
      dependencies.subsetOf(completedRelations + task.outputRelation)
    }

    log.debug(s"${eligibleTasks.size} tasks are eligible. ${notEligibleTasks.size} not eligible.")
    taskQueue ++= notEligibleTasks
    
    eligibleTasks.foreach { task =>
      log.debug(s"executing $task")
      // TODO: Send to worker
      val newWorker = context.actorOf(ExtractorExecutor.props)
      newWorker ! ExtractorExecutor.ExecuteTask(task)
      runningTasks += Tuple2(task, newWorker)
    }
    log.info(s"num=${taskQueue.size} tasks left")
  }
}
