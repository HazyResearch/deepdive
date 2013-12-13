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
    log.debug("Starting")
  }

  def receive = {
    case ExecuteTask(task) => 
      log.debug(s"Executing $task")
      doExecute(task)
      log.debug(s"Finished executing task_name=${task.name}")
      context.parent ! ExtractionManager.TaskCompleted(task)
      context.stop(self)
    case _ =>
      log.warning("Huh?")
  }

  private def doExecute(task: ExtractionTask) {
    val executor = new PostgresScriptTaskExecutor(task)
    val result = executor.run()
    writeResult(result, task.outputRelation)
  }

  private def writeResult(result: ExtractionResult, outputRelation: String) {
    dataStore.writeResult(result.rows, outputRelation)
  }
}

/* Implementation of an ExtractorExecutor that uses postgresql to store extraction results. */
class PostgresExtractorExecutor extends ExtractorExecutor 
  with PostgresExtractionDataStoreComponent

case class PostgresScriptTaskExecutor(task: ExtractionTask) extends ScriptTaskExecutor 
  with PostgresExtractionDataStoreComponent

