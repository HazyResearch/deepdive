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
import scala.concurrent._
import scala.concurrent.duration._


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
    
    val isDone = Promise[ExtractionTaskResult]()

    val result = executor.run()
    result.rows.buffer(dataStore.BatchSize).subscribe(
      rowBatch => dataStore.write(rowBatch.toList, task.extractor.outputRelation),
      exception => {
        val endTime = System.currentTimeMillis
        log.error(exception.toString)
        profiler ! Profiler.ExtractorFailed(task.extractor, startTime, endTime, exception)
        isDone.success(ExtractionTaskResult(task, false))
      },
      () => { 
        val endTime = System.currentTimeMillis
        profiler ! Profiler.ExtractorFinished(task.extractor, startTime, endTime)
        isDone.success(ExtractionTaskResult(task, true))
      }
    )
    Await.result(isDone.future, Duration.Inf)
  }

}


