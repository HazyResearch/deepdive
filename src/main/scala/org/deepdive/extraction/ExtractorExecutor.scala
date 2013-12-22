package org.deepdive.extraction

import anorm._
import akka.actor.{Actor, ActorRef, Props, ActorLogging}
import org.deepdive.settings._
import org.deepdive.Context
import org.deepdive.datastore.{PostgresDataStore => DB}
import org.deepdive.extraction._
import org.deepdive.extraction.datastore._
import org.deepdive.Logging
import org.deepdive.profiling._
import scala.util.{Try, Success, Failure}


object ExtractorExecutor {
  
  // Implementation of an ExtractorExecutor that uses postgresql to store extraction results.
  // TODO: Refactor this
  class PostgresExtractorExecutor extends ExtractorExecutor 
    with PostgresExtractionDataStoreComponent

  def props: Props = Props(classOf[PostgresExtractorExecutor])

  // Messages we can receive
  sealed trait Message
  case class ExecuteTask(task: ExtractionTask)
}

/* Executes a single extraction task, shuts down when done. */
trait ExtractorExecutor extends Actor with ActorLogging  { 
  self: ExtractionDataStoreComponent =>

  import ExtractorExecutor._

  val profiler = context.actorSelection("/user/profiler")

  override def preStart() {
    log.info("Starting")
  }

  def receive = {
    case ExecuteTask(task) => 
      log.info(s"Executing $task")
      val taskResult = doExecute(task)
      log.info(s"Finished executing task_name=${task.extractor.name}")
      context.parent ! taskResult
      context.stop(self)
    case _ =>
      log.warning("Huh?")
  }

  def doExecute(task: ExtractionTask) : ExtractionTaskResult = {
    val startTime = System.currentTimeMillis
    val executor = new ScriptTaskExecutor(task, dataStore.queryAsJson(task.extractor.inputQuery))
    val result = Try(executor.run())
    val endTime = System.currentTimeMillis
    result match {
      case Success(x) =>
        profiler ! Profiler.ExtractorFinished(task.extractor, startTime, endTime)
        dataStore.write(x.rows, task.extractor.outputRelation)
      case Failure(ex) =>
        profiler ! Profiler.ExtractorFailed(task.extractor, startTime, System.currentTimeMillis, ex)
    }
    ExtractionTaskResult(task, result.isSuccess)
  }

}


