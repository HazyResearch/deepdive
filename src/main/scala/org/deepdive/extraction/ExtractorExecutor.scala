package org.deepdive.extraction

import anorm._
import akka.actor.{Actor, ActorRef, Props, ActorLogging}
import org.deepdive.settings._
import org.deepdive.context._
import org.deepdive.datastore.{PostgresDataStore => DB}
import org.deepdive.extraction._
import org.deepdive.extraction.datastore._
import org.deepdive.Logging


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

  override def preStart() {
    log.info("Starting")
  }

  def receive = {
    case ExecuteTask(task) => 
      log.info(s"Executing $task")
      doExecute(task)
      log.info(s"Finished executing task_name=${task.extractor.name}")
      context.parent ! ExtractionManager.TaskCompleted(task)
      context.stop(self)
    case _ =>
      log.warning("Huh?")
  }

  def doExecute(task: ExtractionTask) {
    val executor = new ScriptTaskExecutor(task, this)
    val result = executor.run()
    writeResult(result, task.extractor.outputRelation)
  }

  def writeResult(result: ExtractionResult, outputRelation: String) {
    dataStore.writeResult(result.rows, outputRelation)
  }
}


