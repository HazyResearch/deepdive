package org.deepdive.extraction

import akka.actor.SupervisorStrategy._
import akka.actor.{Actor, ActorRef, Props, ActorLogging, FSM, OneForOneStrategy}
import akka.pattern.{ask, pipe}
import org.deepdive.Context
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
}

/* 
 * Manages extraction tasks. The ExtractionManager is responsible for executing
 * extractions tasks in the correct order. It also parallelizes execution when possible.
 */ 
class ExtractionManager extends Actor with ActorLogging {
  import ExtractionManager._
  
  val EXTRACTOR_TIMEOUT = 30.minutes
  val PARALLELISM = 1
  val RETRY_TIMEOUT = 5.seconds

  // Keeps track of the tasks
  val taskQueue = ArrayBuffer[ExtractionTask]()
  val runningTasks = Map[ExtractionTask, ActorRef]()
  val completedTasks = ArrayBuffer[ExtractionTask]()
  // Who wants to be notified when a task is done?
  val taskListeners = Map[ExtractionTask, ActorRef]()

  override def preStart(){
    log.info("Starting")
  }

  def receive = {
    case AddTask(task) =>
      log.info(s"Adding task_name=${task.extractor.name}")
      taskQueue += task
      taskListeners += Tuple2(task, sender)
      scheduleTasks
    case msg @ ExtractionTaskResult(task, success) =>
      log.info(s"Completed task_name=${task.extractor.name}")
      runningTasks -= task
      completedTasks += task
      // Notify the listener
      taskListeners.get(task).foreach(_ ! msg)
      scheduleTasks
    case msg => log.warning(s"Huh? ($msg)")
  }

  // Schedules new taks based on the queue and capacity
  private def scheduleTasks() : Unit = {

    log.info("scheduling tasks")
    // How many more tasks can we execute in parallel right now?
    val capacity = PARALLELISM - runningTasks.size
    
    // Get the tasks from the queue, but only take those that have all dependencies satisfied
    val completedExtractors = completedTasks.map(_.extractor.name).toSet
    val (eligibleTasks, notEligibleTasks) = taskQueue.partition { x =>
      x.extractor.dependencies.subsetOf(completedExtractors)
    }
    log.info(s"numEligibleTasks=${eligibleTasks.size} numNotEligibleTasks=${notEligibleTasks.size}")
    
    eligibleTasks.take(capacity).foreach { task =>
      log.info(s"executing extractorName=${task.extractor.name}")
      val newWorker = context.actorOf(ExtractorExecutor.props)
      val result = newWorker.ask(ExtractorExecutor.ExecuteTask(task))(EXTRACTOR_TIMEOUT)
      taskQueue -= task
      runningTasks += Tuple2(task, newWorker)
    }
    log.info(s"num=${taskQueue.size} tasks left")
  }

}
