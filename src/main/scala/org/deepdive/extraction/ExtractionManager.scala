package org.deepdive.extraction

import akka.actor.SupervisorStrategy._
import akka.actor._
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import org.deepdive.{Context, TaskManager}
import org.deepdive.settings._
import org.deepdive.extraction._
import scala.collection.mutable.{PriorityQueue, ArrayBuffer, Map}
import scala.concurrent.duration._
import scala.util.{Try, Success, Failure}

/* Companion Object for the Extraction Mangager */
object ExtractionManager {
  def props : Props = Props(classOf[ExtractionManager])
  
  // Messages 
  sealed trait Message
}

/* 
 * Manages extraction tasks. The ExtractionManager is responsible for executing
 * extractions tasks in the correct order. It parallelizes execution when possible.
 */ 
class ExtractionManager extends Actor with ActorLogging {
  import ExtractionManager._
  
  val PARALLELISM = 1
  implicit val ExtractorTimeout = Timeout(24 hours)
  import context.dispatcher
  
  // Keeps track of the tasks
  val taskQueue = ArrayBuffer[ExtractionTask]()
  val listeners = Map[ExtractionTask, ActorRef]()

  override def preStart(){
    log.info("starting")
  }

  def receive = {
    case task : ExtractionTask =>
      log.info(s"Adding task_name=${task.extractor.name}")
      taskQueue += task
      listeners += Tuple2(task, sender)
      scheduleTasks()
    case ExtractionTaskResult(task, result) =>
      log.info(s"Completed task_name=${task.extractor.name}")
      listeners.get(task).foreach(_ ! result)
      scheduleTasks()
    case Terminated(worker) =>
      scheduleTasks()
    case msg => 
      log.warning(s"Huh? ($msg)")
  }

  // Schedules new taks based on the queue and capacity
  private def scheduleTasks() : Unit = {

    // How many more tasks can we execute in parallel right now?
    val capacity = PARALLELISM - context.children.size
    
    taskQueue.take(capacity).foreach { task =>
      log.info(s"executing extractorName=${task.extractor.name}")
      val newWorker = context.actorOf(ExtractorExecutor.props)
      val result = newWorker ? ExtractorExecutor.ExecuteTask(task) pipeTo self
      context.watch(newWorker)
      taskQueue -= task
    }
  }

}
