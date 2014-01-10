package org.deepdive.extraction

import akka.actor._
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
import spray.json._
import scala.util.Random


object ExtractorRunner {
  
  def props(dataStore: JsonExtractionDataStore) = Props(classOf[ExtractorRunner], dataStore)

  // Messages
  sealed trait Message
  case class SetTask(task: ExtractionTask) extends Message
  case class WriteData(data: Seq[String]) extends Message
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
  case class Task(task: ExtractionTask, sender: ActorRef, workers: Seq[ActorRef]) extends Data

}

class ExtractorRunner(dataStore: JsonExtractionDataStore) extends Actor 
  with ActorLogging with FSM[State, Data] {

  import ExtractorRunner._
  import context.dispatcher

  def workerProps = ProcessExecutor.props

  override def preStart() { log.info("waiting for task") }

  // Start in this state
  startWith(Idle, Uninitialized)

  when(Idle) {
    case Event(SetTask(task), Uninitialized) =>
      log.info(s"Received task=${task.extractor.name}. Executing")
      
      // Execute the before script. Fail if the script fails.
      task.extractor.beforeScript.map(executeCmd) match {
        case (Some(Success(_)) | None) => // All good
        case Some(Failure(exception)) => 
          log.error(exception.toString) 
          sender ! Status.Failure(exception)
          context.stop(self)
      }

      // Start the children workers
      val workers = startWorkers(task)
      // Schedule the input data to be sent to myself
      Future { sendData(task) }
      goto(Running) using Task(task, sender, workers)
  }

  when(Running) {
    case Event(WriteData(chunk), Task(task, sender, workers)) =>
      // Pick a random child to receive the data
      val randomWorker = workers(Random.nextInt(workers.size))
      log.debug(s"sending data chunk of size=${chunk.size} to worker=${randomWorker.path.name}")
      randomWorker ! ProcessExecutor.Write(chunk.mkString("\n"))
      stay
    case Event(Terminated(actor), Task(task, sender, workers)) =>
      log.debug(s"worker=${actor.path.name} has terminated")
      val newWorkers = workers.filterNot(_ == actor)
      newWorkers.size match {
        case 0 => 
          log.info(s"All workers are done. Finishing up.")
          self ! ExecuteAfterScript
          self ! Shutdown
          dataStore.flushBatches(task.extractor.outputRelation)
          goto(Finishing) using(Task(task, sender, newWorkers))
        case _ => 
          stay using(Task(task, sender, newWorkers)) 
      }
    case Event(AllDataDone, Task(task, sender, workers)) =>
      log.info("Sent all data to workers. Waiting for them to terminate")
      workers.foreach ( _ ! ProcessExecutor.CloseInputStream )
      stay
    case Event(ProcessExecutor.OutputData(chunk), Task(task, taskSender, workers)) =>
      // log.debug(s"adding chunk of size=${chunk.size} data store: ${chunk.toString}")
      val jsonData = chunk map (_.asJson.asInstanceOf[JsObject])
      Future { dataStore.addBatch(jsonData, task.extractor.outputRelation) } pipeTo sender
      stay
    case Event(ProcessExecutor.ProcessExited(exitCode), Task(task, sender, workers)) =>
      if (exitCode == 0) {
        stay
      } else {
        sender ! Status.Failure(new RuntimeException(s"process exited with exit_code=${exitCode}"))
        stop
      } 
  }

  when(Finishing) {
    case(Event(ExecuteAfterScript, Task(task, taskSender, workers))) =>
      // Execute the after script. Fail if the script fails.
      task.extractor.afterScript.map(executeCmd) match {
        case (Some(Success(_)) | None) => // All good
        case Some(Failure(exception)) => 
          log.error(exception.toString) 
          taskSender ! Status.Failure(exception)
          context.stop(self)
      }
      stay
    case(Event(Shutdown, Task(task, taskSender, workers))) =>
      log.info(s"Shutting down")
      taskSender ! "Done!"
      stop
  }

  private def startWorkers(task: ExtractionTask) : Seq[ActorRef] = {
    log.info(s"Starting ${task.extractor.parallelism} children process workers")
    // Start workers accoridng tot he specified parallelism
    (1 to task.extractor.parallelism).map { i =>
      val worker = context.actorOf(workerProps, s"processExecutor${i}")
      context.watch(worker)
      worker ! ProcessExecutor.Start(task.extractor.udf, task.extractor.outputBatchSize)
      worker
    }.toSeq
  }

  private def sendData(task: ExtractionTask) {
    log.info(s"Getting data from the data store")

    // Figure out where to get the input from
    val extractorInput = task.extractor.inputQuery match {
      case CSVInputQuery(filename, seperator) =>
        FileDataUtils.queryAsJson[Unit](filename, seperator)_
      case DatastoreInputQuery(query) =>
        dataStore.queryAsJson[Unit](query)_
    }

    // Send the input to myself
    extractorInput { iterator =>
      iterator map(_.toString) grouped(task.extractor.inputBatchSize) foreach { chunk =>
        self ! WriteData(chunk)
      }
    }
    self ! AllDataDone
  }


  // Executes a cmd command and returns the exit value if it succeeds
  def executeCmd(cmd: String) : Try[Int] = {
    log.info(s"""Executing: "$cmd" """)
    Try(cmd!(ProcessLogger(line => log.info(line)))) match {
      case Success(0) => Success(0)
      case Success(errorExitValue) => 
        Failure(new RuntimeException(s"Script exited with exit_value=$errorExitValue"))
      case Failure(ex) => Failure(ex)
    }
  } 




}