package org.deepdive.extraction

import akka.actor._
import org.deepdive.settings._
import org.deepdive.Context
import org.deepdive.datastore.{PostgresDataStore => DB}
import org.deepdive.extraction._
import org.deepdive.extraction.datastore._
import org.deepdive.Logging
import scala.util.{Try, Success, Failure}
import scala.concurrent._
import scala.concurrent.duration._
import scala.sys.process._
import rx.lang.scala.subjects._
import spray.json.JsValue


object ExtractorExecutor {
  
  def props(dataStore: ExtractionDataStoreComponent#ExtractionDataStore[_ <: JsValue]): Props = 
    Props(classOf[ExtractorExecutor], dataStore)

  // Messages we can receive
  sealed trait Message
  case class ExecuteTask(task: ExtractionTask)
}

/* Executes a single extraction task, shuts down when done. */
class ExtractorExecutor(dataStore: ExtractionDataStoreComponent#ExtractionDataStore[_ <: JsValue]) 
  extends Actor with ActorLogging  { 

  import ExtractorExecutor._

  override def preStart() {
    log.info("Starting")
  }

  def receive = {
    case ExecuteTask(task) => 
      log.info(s"Executing $task")
      val taskResult = doExecute(task)
      log.info(s"Finished executing task_name=${task.extractor.name}")
      taskResult match {
        case Success(x) => sender ! x
        case Failure(ex) => sender ! Status.Failure(ex)
      }
      context.stop(self)
    case _ =>
      log.warning("Huh?")
  }

  def executeCmd(cmd: String) : Try[Int] = {
    log.info(s"""Executing: "$cmd" """)
    Try(cmd!(ProcessLogger(line => log.info(line)))) match {
      case Success(0) => Success(0)
      case Success(errorExitValue) => 
        Failure(new RuntimeException(s"Script exited with exit_value=$errorExitValue"))
      case Failure(ex) => Failure(ex)
    }
  } 

  def executeUDF(task: ExtractionTask) : Try[ExtractionTaskResult] = {
    val extractorInput = task.extractor.inputQuery match {
      case CSVInputQuery(filename, seperator) =>
        FileDataUtils.queryAsJson[Try[ExtractionTaskResult]](filename, seperator)_
      case DatastoreInputQuery(query) =>
        dataStore.queryAsJson[Try[ExtractionTaskResult]](query)_
    }

    extractorInput { iterator =>
      val executor = new ScriptTaskExecutor(task, iterator)
      val result = executor.run()
    
      // We execute writing to the database asynchronously because it may be a long operation
      // TODO: We are using Akka's default dispatcher here, maybe we should define our own.
      val writtenResults = result.rows.flatMap { rowBatch =>
        import context.dispatcher
        val writeFuture = Future { dataStore.addBatch(rowBatch, task.extractor.outputRelation) }
        val subject = AsyncSubject[Unit]()
        writeFuture onComplete {
          case Failure(x) => { subject.onError(x) }
          case Success(x) => { subject.onNext(x); subject.onCompleted() }
        }
        subject
      }

      // Handle the results of writing back to the database.
      // Block until all results are written
      val isDone = Promise[Try[ExtractionTaskResult]]()
      writtenResults.subscribe(
        rowBatch => {},
        exception => {
          log.error(exception.toString)
          isDone.success(Failure(exception))
        },  
        () => {
          dataStore.flushBatches(task.extractor.outputRelation)
          isDone.success(Success(ExtractionTaskResult(task.extractor.name)))
        }
      )
      Await.result(isDone.future, Duration.Inf)
    }
  }

  def doExecute(task: ExtractionTask) : Try[ExtractionTaskResult] = {
    
    // Execute the before script if specified
    task.extractor.beforeScript.map(executeCmd) match {
      case (Some(Success(_)) | None) => // All good
      case Some(Failure(exception)) => 
        log.error(exception.toString) 
        return Failure(exception)
    }

    val extractionResult = executeUDF(task)

    // Execute the after script if specified
    task.extractor.afterScript.map(executeCmd) match {
      case (Some(Success(_)) | None) => // All good
      case Some(Failure(exception)) => 
        log.error(exception.toString) 
        return Failure(exception)
    }

    extractionResult
  }

}


