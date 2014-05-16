package org.deepdive.extraction

import akka.actor.SupervisorStrategy._
import akka.actor._
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import org.deepdive.{Context, TaskManager}
import org.deepdive.settings._
import org.deepdive.extraction._
import org.deepdive.extraction.datastore._
import scala.collection.mutable.{PriorityQueue, ArrayBuffer, Map}
import scala.concurrent.duration._
import scala.util.{Try, Success, Failure}

/* Companion Object for the Extraction Mangager */
object ExtractionManager {

  // TODO: Refactor this to take an argument for the dataStore type
  def props(parallelism: Int, dbSettings: DbSettings) : Props = {
    dbSettings.driver match {
      case "org.postgresql.Driver" => Props(classOf[PostgresExtractionManager], parallelism, dbSettings)
    }
  }

  class PostgresExtractionManager(val parallelism: Int, val dbSettings: DbSettings) extends ExtractionManager
    with PostgresExtractionDataStoreComponent

  case object ScheduleTasks

  // Messages 
  sealed trait Message
}

/* 
 * Manages extraction tasks. The ExtractionManager is responsible for executing
 * extractions tasks in the correct order. It parallelizes execution when possible.
 */ 
trait ExtractionManager extends Actor with ActorLogging {
  this: ExtractionDataStoreComponent =>

  import ExtractionManager._

  // Number of executors we can run in parallel
  def parallelism : Int
  def dbSettings: DbSettings

  def extractorRunnerProps = ExtractorRunner.props(dataStore, dbSettings)


  implicit val ExtractorTimeout = Timeout(200 hours)
  import context.dispatcher
  
  // Keeps track of the tasks
  val taskQueue = Map[ExtractionTask, ActorRef]()

  override def preStart(){
    log.info("starting")
    dataStore.init()
  }

  def receive = {
    case task : ExtractionTask =>
      log.info(s"Adding task_name=${task.extractor.name}")
      taskQueue += Tuple2(task, sender)
      self ! ScheduleTasks
    case ScheduleTasks =>
      scheduleTasks()

    case Terminated(worker) =>
      self ! ScheduleTasks
  }

  // Schedules new taks based on the queue and capacity
  private def scheduleTasks() : Unit = {

    // How many more tasks can we execute in parallel right now?
    val capacity = parallelism - context.children.size
    
    taskQueue.take(capacity).foreach { case(task, sender) =>
      log.info(s"executing extractorName=${task.extractor.name}")
      val newWorker = context.actorOf(extractorRunnerProps, s"extractorRunner-${task.extractor.name}")
      val result = newWorker ? ExtractorRunner.SetTask(task) pipeTo sender
      context.watch(newWorker)
      taskQueue -= task
    }
  }

}
