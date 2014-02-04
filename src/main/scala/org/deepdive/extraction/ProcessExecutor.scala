package org.deepdive.extraction

import akka.actor._
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import java.io.{File, OutputStream, InputStream, PrintWriter, BufferedWriter, OutputStreamWriter, Writer}
import ProcessExecutor._
import scala.collection.mutable.ArrayBuffer
import scala.concurrent._
import scala.concurrent.duration._
import scala.io.Source
import scala.sys.process._

/* Companion object to process executor */
object ProcessExecutor {
  
  // Start the actor using this method
  def props = Props(classOf[ProcessExecutor])
  
  // Received Messages
  // ==================================================

  // Start the process with a given command. Output lines are returnd in batchSize batches.
  case class Start(cmd: String, outputBatchSize: Int)
  // Write data to stdin of the started process
  case class Write(data: String)
  // Close the input stream of the started process
  case object CloseInputStream
  // Signal that the process has exited (self message)
  case class ProcessExited(exitValue: Int)

  // Sent Messages
  // An output batch from the process
  case class OutputData(block: List[String])

  // States
  sealed trait State
  // The actor is waiting for someone to tell a process command
  case object Idle extends State
  // The actor is running a process and receiving data
  case object Running extends State

  // Data (Internal State)
  sealed trait Data
  case object Uninitialized extends Data
  case class TaskInfo(cmd: String, outputBatchSize: Int, sender: ActorRef)
  case class ProcessInfo(process: Process, inputStream: PrintWriter,
    errorStream: InputStream, outputStream: InputStream)
  case class RuntimeData(processInfo: ProcessInfo, taskInfo: TaskInfo) extends Data

}

/* Executes a process and channels input and output data */
class ProcessExecutor extends Actor with FSM[State, Data] with ActorLogging {

  /* We use the current dispatcher for Futures */
  import context.dispatcher
  implicit val taskTimeout = Timeout(24 hours)

  override def preStart() { log.info("started") }

  startWith(Idle, Uninitialized)

  when(Idle) {
    case Event(Start(cmd, outputBatchSize), Uninitialized) =>
      // Start the process (don't close over the sender)
      val _sender = sender
      val processInfo = startProcess(cmd, outputBatchSize, _sender)
      // Schedule a message when the process has exited
      Future { 
        val exitValue = processInfo.process.exitValue()
        self ! ProcessExited(exitValue) 
        processInfo.process.destroy()
      }
      // Transition to the running state
      goto(Running) using RuntimeData(processInfo, TaskInfo(cmd, outputBatchSize, sender))
  }

  when(Running) {
    case Event(Write(data), RuntimeData(processInfo, taskInfo)) =>
      // Write data to the process. Do this in a different thread
      processInfo.inputStream.println(data)
      sender ! "OK!"
      stay
    case Event(CloseInputStream, RuntimeData(processInfo, taskInfo)) =>
      // Close the input stream. 
      // No more data goes to the process. We still need to wait for the process to exit.
      log.debug(s"closing input stream")
      processInfo.inputStream.close()
      stay
    case Event(ProcessExited(exitValue), RuntimeData(processInfo, taskInfo)) =>
      // The process has exited, notify the task sender and stop
      log.info(s"process exited with exit_value=$exitValue")
      taskInfo.sender ! ProcessExited(exitValue)
      stop()
  }

  initialize()

  /* Starts the process, blocks until all streams are connected, then return a ProcessInfo */
  private def startProcess(cmd: String, batchSize: Int, dataCallback: ActorRef) : ProcessInfo = {
    val inputStreamFuture = Promise[PrintWriter]()
    val errorStreamFuture = Promise[InputStream]()
    val outputStreamFuture = Promise[InputStream]()

    log.info(s"""starting process with cmd="$cmd" and batch_size=$batchSize""")
    // Build a process object and handle the input, output, and error streams
    val processBuilder = new ProcessIO(
      in => {
        val writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(in)))
        inputStreamFuture.success(writer)
      },  
      out => {
        outputStreamFuture.success(out)
        Source.fromInputStream(out).getLines.grouped(batchSize).foreach { batch =>
          // We wait for the result here, because we don't want to read too much data at once
          Await.result(dataCallback ? OutputData(batch.toList), 1.hour)
        }
        log.debug(s"closing output stream")
        out.close()
      },
      err => { 
        errorStreamFuture.success(err)
        Source.fromInputStream(err).getLines foreach (log.debug)
      }
    )

    // If the process is a file, make it executable
    val file = new File(cmd)
    if (file.isFile) file.setExecutable(true, false)

    // Run the process
    val process = cmd run (processBuilder)

    // Wait for all streams to be connected
    val processInfoFuture = for {
      inputStream <- inputStreamFuture.future
      errorStream <- errorStreamFuture.future
      outputStream <- outputStreamFuture.future
    } yield ProcessInfo(process, inputStream, errorStream, outputStream)

    Await.result(processInfoFuture, 15.seconds)
  }

}

