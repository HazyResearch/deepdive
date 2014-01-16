package org.deepdive.extraction

import akka.actor._
import akka.routing._
import akka.pattern.{ask, pipe}
import org.deepdive.settings._
import org.deepdive.Context
import org.deepdive.extraction._
import org.deepdive.extraction.ExtractorRunner._
import org.deepdive.extraction.datastore._
import org.deepdive.extraction.datastore.ExtractionDataStore._
import org.deepdive.Logging
import scala.util.{Try, Success, Failure}
import scala.concurrent._
import scala.concurrent.duration._
import scala.sys.process._
import rx.lang.scala.subjects._
import play.api.libs.json._
import scala.util.Random

/* Companion object to the ExtractorRunner */
object ExtractorRunner {
  
  def props(dataStore: JsonExtractionDataStore) = Props(classOf[ExtractorRunner], dataStore)

  // Messages
  sealed trait Message
  case class SetTask(task: ExtractionTask) extends Message
  case class RouteData(data: List[String]) extends Message
  case object AllDataDone extends Message
  case object ExecuteAfterScript
  case object Shutdown

  // States
  sealed trait State
  case object Idle extends State
  case object Running extends State
  case object Finishing extends State

  // Data
  sealed trait Data
  case object Uninitialized extends Data
  case class Task(task: ExtractionTask, sender: ActorRef, workers: Router) extends Data

}

/* Runs a single extrator by executing its before script, UDF, and after sript */
class ExtractorRunner(dataStore: JsonExtractionDataStore) extends Actor 
  with ActorLogging with FSM[State, Data] {

  import ExtractorRunner._
  // Execute futures using the current Akka dispatcher
  import context.dispatcher
  
  // Properties to start workers
  def workerProps = ProcessExecutor.props

  override def preStart() { log.info("waiting for task") }

  // Start in the idle state
  startWith(Idle, Uninitialized)

  when(Idle) {
    case Event(SetTask(task), Uninitialized) =>
      log.info(s"Received task=${task.extractor.name}. Executing")
      
      // Execute the before script. Fail if the script fails.
      task.extractor.beforeScript.foreach { beforeScript =>
        log.info("Executing before script.")
        executeScriptOrFail(beforeScript, sender)
      }
      
      // Start the children workers
      val workers = startWorkers(task)
      // Schedule the input data to be sent to myself.
      // We will then forward the data to our workers
      Future { sendData(task, workers) }
      goto(Running) using Task(task, sender, workers)
  }

  when(Running) {
    
    case Event(Terminated(actor), Task(task, taskSender, workers)) =>
      // A worker has terminated, remove it from our list
      log.debug(s"worker=${actor.path.name} has terminated")
      val newWorkers = workers.removeRoutee(actor)
      // If we have no workers left, move to the next state
      newWorkers.routees.size match {
        case 0 => 
          log.info(s"All workers are done. Finishing up.")
          self ! ExecuteAfterScript
          self ! Shutdown
          goto(Finishing) using(Task(task, taskSender, newWorkers))
        case _ => 
          stay using(Task(task, taskSender, newWorkers)) 
      }
    
    case Event(ProcessExecutor.OutputData(chunk), Task(task, taskSender, workers)) =>
      // Don't close over this
      val _sender = sender
      // We write the data to the data store, asynchronously
      Future {
        log.debug(s"adding chunk of size=${chunk.size} data store.")
        val jsonData = chunk.map(Json.parse).map(_.asInstanceOf[JsObject])
        dataStore.addBatch(jsonData.iterator, task.extractor.outputRelation) 
      }.map ( x => "OK!") pipeTo _sender
      stay
    
    case Event(ProcessExecutor.ProcessExited(exitCode), Task(task, taskSender, workers)) =>
      // A worker process has exited. If successful, continue.
      // If the process failed, shutdown and respond with failure
      exitCode match {
        case 0 => stay
        case exitCode => 
          taskSender ! Status.Failure(new RuntimeException(s"process exited with exit_code=${exitCode}"))
          stop
      }
  }

  when(Finishing) {
    case(Event(ExecuteAfterScript, Task(task, taskSender, workers))) =>
      // Execute the after script. Fail if the script fails.
      task.extractor.afterScript.foreach { afterScript =>
        log.info("Executing after script.")
        executeScriptOrFail(afterScript, taskSender)
      }
      stay
    case(Event(Shutdown, Task(task, taskSender, workers))) =>
      // All done, shutting down
      log.info(s"Shutting down")
      taskSender ! "Done!"
      stop
  }

  /* Starts all workers, watches them, and returns a round-robin fashion router */
  private def startWorkers(task: ExtractionTask) : Router = {
    log.info(s"Starting ${task.extractor.parallelism} children process workers")
    // Start workers accoridng tot he specified parallelism
    val workers = (1 to task.extractor.parallelism).map { i =>
      val worker = context.actorOf(workerProps, s"processExecutor${i}")
      // Deathwatch
      context.watch(worker)
      ActorRefRoutee(worker)
    }
    val router = Router(RoundRobinRoutingLogic(), workers)
    // Send start broadcast to all workers
    val startMessage = ProcessExecutor.Start(task.extractor.udf, task.extractor.outputBatchSize)
    router.route(Broadcast(startMessage), self)
    router
  }

  /* Queries the data store and gets all the data */
  private def sendData(task: ExtractionTask, workers: Router) {
    log.info(s"Getting data from the data store and sending it to the workers")

    // Figure out where to get the input from
    val extractorInput = task.extractor.inputQuery match {
      case CSVInputQuery(filename, seperator) =>
        FileDataUtils.queryAsJson[Unit](filename, seperator)_
      case DatastoreInputQuery(query) =>
        dataStore.queryAsJson[Unit](query)_
    }

    // Send the input to myself, we will forward it to the workers
    extractorInput { iterator =>
      iterator map(_.toString) grouped(task.extractor.inputBatchSize) foreach { chunk =>
        workers.route(ProcessExecutor.Write(chunk.mkString("\n")), self)
      }
    }
    
    // Notify all workers that they don't receive more data
    workers.route(Broadcast(ProcessExecutor.CloseInputStream), self)
    log.debug("all data was sent to workers.")
  }

  // Executes a given command. If it fails, shutdown and respond to the sender with failure.
  private def executeScriptOrFail(script: String, failureReceiver: ActorRef) : Unit = {
    executeCmd(script) match {
      case Success(_) => // All good. We're done
      case Failure(exception) =>
        log.error(exception.toString) 
        failureReceiver ! Status.Failure(exception)
        context.stop(self)
        throw new RuntimeException(exception.toString)
    }
  }

  /* 
   * Executes a command.
   * Returns Success if the process exists with exit value 0.
   * Returns failure of the process fails, or returns exit value != 0.
   */
  def executeCmd(cmd: String) : Try[Int] = {
    log.info(s"""Executing: "$cmd" """)
    val processLogger = ProcessLogger(line => log.info(line))
    Try(cmd!(processLogger)) match {
      case Success(0) => Success(0)
      case Success(errorExitValue) => 
        Failure(new RuntimeException(s"Script exited with exit_value=$errorExitValue"))
      case Failure(ex) => Failure(ex)
    }
  } 




}