package org.deepdive.extraction

import akka.actor._
import akka.routing._
import akka.pattern.{ask, pipe}
import akka.util.Timeout
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
import java.io.{File, PrintWriter}
// import java.nio.file.Files
import scala.io.Source

/* Companion object to the ExtractorRunner */
object ExtractorRunner {
  
  def props(dataStore: JsonExtractionDataStore, dbSettings: DbSettings) = Props(classOf[ExtractorRunner], dataStore, dbSettings)


  // Messages
  sealed trait Message
  case class SetTask(task: ExtractionTask) extends Message
  case class RouteData(data: List[String]) extends Message
  case object AllDataDone extends Message
  case object ExecuteAfterScript
  case object Shutdown
  case object PrintStatus

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
class ExtractorRunner(dataStore: JsonExtractionDataStore, dbSettings: DbSettings) extends Actor 
  with ActorLogging with FSM[State, Data] {

  import ExtractorRunner._
  // Execute futures using the current Akka dispatcher
  import context.dispatcher
  implicit val timeout = Timeout(1337.hours)

  // Generate SQL query prefixes
  val dbname = dbSettings.dbname
  val pguser = dbSettings.user
  val pgport = dbSettings.port
  val pghost = dbSettings.host
  // TODO do not use password for now
  val dbnameStr = dbname match {
    case null => ""
    case _ => s" -d ${dbname} "
  }
  val pguserStr = pguser match {
    case null => ""
    case _ => s" -U ${pguser} "
  }
  val pgportStr = pgport match {
    case null => ""
    case _ => s" -p ${pgport} "
  }
  val pghostStr = pghost match {
    case null => ""
    case _ => s" -h ${pghost} "
  }
  val sqlQueryPrefix = "psql " + dbnameStr + pguserStr + pgportStr + pghostStr

  
  // Properties to start workers
  def workerProps = ProcessExecutor.props

  // Periodically print the status
  val scheduledStatus = context.system.scheduler.schedule(30.seconds, 30.seconds, self, PrintStatus)

  override def preStart() { 
    log.info("waiting for tasks")
  }

  override def postStop() {
    scheduledStatus.cancel()
  }

  // Start in the idle state
  startWith(Idle, Uninitialized)

  when(Idle) {
    case Event(SetTask(task), Uninitialized) =>
      log.info(s"Received task=${task.extractor.name}. Executing")
      
      val taskSender = sender

      // Execute the before script. Fail if the script fails.
      task.extractor.beforeScript.foreach {
        beforeScript =>
          log.info("Executing before script.")
          executeScriptOrFail(beforeScript, taskSender)
      }

      task.extractor.style match {

        case "json_extractor" =>
          // Start the children workers
          val workers = startWorkers(task)

          // Schedule the input data to be sent to myself.
          // We will then forward the data to our workers
          Future { sendData(task, workers, taskSender) }
          goto(Running) using Task(task, sender, workers)

        // Zifei: New code path for faster extraction
        //   Get rid of scala file operations
        //   COPY to a file, split files, and send to extractors
        case "tsv_extractor" =>

          val udfCmd = task.extractor.udf
          val funcName = s"func_${task.extractor.name}"
          
          val inputQuery = task.extractor.inputQuery match {
            case DatastoreInputQuery(query) => query
            case _ => 
          }
          val queryOutputFile = File.createTempFile(s"copy_query_${funcName}", ".tsv")
          // executeSqlQueryOrFail

          // Single-thread copy to a file
          val copyQuery = "COPY (" + s"${inputQuery}".replaceAll("""(?m)[;\s\n]+$""", "") + ") TO STDOUT;"
          log.info(s"Copying file into ${queryOutputFile}")
          executeSqlQueryOrFail(copyQuery, taskSender, queryOutputFile.getAbsolutePath())

          val fname = queryOutputFile.getName()
          val fpath = queryOutputFile.getParent()

          val splitPrefix = queryOutputFile.getAbsolutePath() + "-"
          val linesPerSplit = task.extractor.inputBatchSize
          val splitCmd = s"split -a 10 -l ${linesPerSplit} " + queryOutputFile.getAbsolutePath() + s" ${splitPrefix}"

          log.info(s"Executing split command: ${splitCmd}")
          executeScriptOrFail(splitCmd, taskSender)

          // val maxParallel = "0"  // As many as possible, which is dangerous
          val maxParallel = task.extractor.parallelism

          // Note (msushkov): the extractor must take TSV as input and produce TSV as output
          val runCmd = s"find ${fpath} -name '${fname}-*' 2>/dev/null -print0 | xargs -0 -P ${maxParallel} -L 1 bash -c '${udfCmd} " + "<" + " \"$0\" > \"$0.out\"'"

          log.info(s"Executing parallel UDF command: ${runCmd}")
          // executeScriptOrFail(runCmd, taskSender)

          val udfTmpFile = File.createTempFile(s"exec_parallel_udf", ".sh")
          val writer = new PrintWriter(udfTmpFile)
          writer.println(s"${runCmd}")
          writer.close()
          log.debug(s"Temporary UDF file saved to ${udfTmpFile.getAbsolutePath()}")
          executeScriptOrFail(udfTmpFile.getAbsolutePath(), taskSender)

          // Copy each of the files into the DB. If user is using Greenplum, use gpload

          val outputRel = task.extractor.outputRelation
          val dbname = dbSettings.dbname
          val pguser = dbSettings.user
          val pgport = dbSettings.port
          val pghost = dbSettings.host
          // TODO do not use password for now

          val checkGreenplumSQL = s"""
              SELECT version() LIKE '%Greenplum%';
            """


          // Only allow single-threaded copy
          val writebackCmd = s"find ${splitPrefix}*.out -print0 | xargs -0 -P 1 -L 1 bash -c 'psql -d ${dbname} -U ${pguser} -p ${pgport} -h ${pghost} -c " + "\"COPY " + s"${outputRel} FROM STDIN;" + " \" < \"$0\"'"



          val writebackTmpFile = File.createTempFile(s"exec_parallel_writeback", ".sh")
          val writer2 = new PrintWriter(writebackTmpFile)
          writer2.println(s"${writebackCmd}")
          writer2.close()
          log.debug(s"Temporary writeback file saved to ${writebackTmpFile.getAbsolutePath()}")
          executeScriptOrFail(writebackTmpFile.getAbsolutePath(), taskSender)

          log.info("Analyzing output relation.")
          executeSqlUpdateOrFail(s"ANALYZE ${outputRel};", taskSender)

          log.info("Removing temporary files...")
          queryOutputFile.delete()
          // val delCmd = s"rm -f ${splitPrefix}*"
          // prevent "argument list too long"
          
          val delCmd = s"find ${fpath} -name '${fname}*' 2>/dev/null -print0 | xargs -0 rm -f"
          log.info(s"Executing: ${delCmd}")
          val delTmpFile = File.createTempFile(s"exec_delete", ".sh")
          val delWriter = new PrintWriter(delTmpFile)
          delWriter.println(s"${delCmd}")
          delWriter.close()
          executeScriptOrFail(delTmpFile.getAbsolutePath(), taskSender)
          executeScriptOrFail(delTmpFile.getAbsolutePath(), taskSender)
          delTmpFile.delete()



          // Execute the after script. Fail if the script fails.
          task.extractor.afterScript.foreach {
            afterScript =>
              log.info("Executing after script.")
              executeScriptOrFail(afterScript, taskSender)
          }

          taskSender ! "Done!"
          stop




        // Execute the sql query from sql extractor
        case "sql_extractor" =>
          log.debug("Executing SQL query: ${task.extractor.sqlQuery}")
          executeSqlUpdateOrFail(task.extractor.sqlQuery, taskSender)
          // Execute the after script. Fail if the script fails.
          task.extractor.afterScript.foreach {
            afterScript =>
              log.info("Executing after script.")
              executeScriptOrFail(afterScript, taskSender)
          }
          taskSender ! "Done!"
          stop

        case "cmd_extractor" =>
          task.extractor.cmd.foreach {
            cmd =>
              log.info("Executing cmd script.")
              executeScriptOrFail(cmd, taskSender)
          }
          // Execute the after script. Fail if the script fails.
          task.extractor.afterScript.foreach {
            afterScript =>
              log.info("Executing after script.")
              executeScriptOrFail(afterScript, taskSender)
          }
          taskSender ! "Done!"
          stop

        case "plpy_extractor" =>

          // Try to create language; if exists do nothing; if other errors report
          executeSqlQueryOrFail("drop language if exists plpythonu cascade; CREATE LANGUAGE plpythonu;", taskSender)
          
          // Create Function in GP
          val udfFile = task.extractor.udf
          val deepDiveDir = System.getProperty("user.dir")
          val compilerFile = s"${deepDiveDir}/util/ddext.py"
          val funcName = s"func_${task.extractor.name}"
          val sqlFunctionFile = File.createTempFile(funcName, ".sql")
          
          executeScriptOrFail(s"python ${compilerFile} ${udfFile} ${sqlFunctionFile} ${funcName}", taskSender)
          log.debug(s"Compiled ${udfFile} into ${sqlFunctionFile}")

          // Source.fromFile(sqlFunctionFile).getLines.mkString
          executeSqlFileOrFail(sqlFunctionFile.getAbsolutePath(), taskSender)

          // Translate SQL input and output_relation to SQL
          val inputQuery = task.extractor.inputQuery match {
            case DatastoreInputQuery(query) => query
            case _ => 
          }
          val inputQueryFile = File.createTempFile(s"query_${funcName}", ".sql")
          val writer = new PrintWriter(inputQueryFile)
          writer.println(inputQuery)
          writer.close()

          val outputRel = task.extractor.outputRelation
          val SQLTranslatorFile = s"${deepDiveDir}/util/ddext_input_sql_translator.py"
          val sqlInsertFile = File.createTempFile(s"${funcName}_exec", ".sql")
          executeScriptOrFail(s"python ${SQLTranslatorFile} ${udfFile} ${inputQueryFile} ${outputRel} ${funcName} ${sqlInsertFile}", taskSender)

          log.debug(s"Compiled query into: ${sqlInsertFile}")

          // Execute query in parallel in GP
          executeSqlFileOrFail(sqlInsertFile.getAbsolutePath(), taskSender)
          log.info(s"Finish executing UDF in database!")

          log.debug("Analyzing output relation.")
          executeSqlUpdateOrFail(s"ANALYZE ${outputRel};", taskSender)

          // Execute the after script. Fail if the script fails.
          task.extractor.afterScript.foreach {
            afterScript =>
              log.info("Executing after script.")
              executeScriptOrFail(afterScript, taskSender)
          }

          taskSender ! "Done!"
          stop

      }

  }

  when(Running) {
    
    case Event(Terminated(actor), Task(task, taskSender, workers)) =>
      // A worker has terminated, remove it from our list
      val newWorkers = workers.removeRoutee(actor)
      log.debug(s"worker=${actor.path.name} has terminated. Waiting for ${newWorkers.routees.size} others.")
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
      }.onComplete {
        case Success(_) => _sender ! "OK!"
        case Failure(exception) =>
          taskSender ! Status.Failure(exception)
          context.stop(self)
          throw exception
      }
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

    case Event(PrintStatus, Task(task, taskSender, workers)) =>
      log.info(s"Status: ${workers.routees.size} workers are running.")
      stay
  }

  when(Finishing) {
    case(Event(ExecuteAfterScript, Task(task, taskSender, workers))) =>
      // Execute the after script. Fail if the script fails.
      task.extractor.afterScript.foreach {
        afterScript =>
          log.info("Analyzing output relation.")
          executeSqlUpdateOrFail(s"ANALYZE ${task.extractor.outputRelation};", taskSender)
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
    // Start workers according to the specified parallelism
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
  private def sendData(task: ExtractionTask, workers: Router, taskSender: ActorRef) {
    log.info(s"Getting data from the data store and sending it to the workers. query='${task.extractor.inputQuery}'")

    // Figure out where to get the input from
    val extractorInput = task.extractor.inputQuery match {
      case CSVInputQuery(filename, seperator) =>
        FileDataUtils.queryAsJson[Unit](filename, seperator) _
      case DatastoreInputQuery(query) =>
        val totalBatchSize = workers.routees.size * task.extractor.inputBatchSize
        dataStore.queryAsJson[Unit](query, Option(totalBatchSize)) _
    }

    // Forward output to the workers
    try {
      extractorInput {
        iterator =>
          val batchSize = workers.routees.size * task.extractor.inputBatchSize
          iterator map (_.toString) grouped (batchSize) foreach {
            chunk =>
              val futures = chunk.grouped(task.extractor.inputBatchSize).map {
                batch =>
                  val msg = ProcessExecutor.Write(batch.mkString("\n"))
                  val destinationWorker = workers.logic.select(msg, workers.routees).asInstanceOf[ActorRefRoutee].ref
                  destinationWorker ? msg
              }
              val allRouteeAcks = Future.sequence(futures)
              // Wait for all workers to write the data to the output stream to avoid overloading them
              Await.result(allRouteeAcks, 1337.hours)
          }
      }
    } catch {
      case exception: Throwable =>
        log.error(exception.toString)
        taskSender ! Status.Failure(exception)
        context.stop(self)
        throw exception
    }

    // Notify all workers that they don't receive more data
    workers.route(Broadcast(ProcessExecutor.CloseInputStream), self)
    log.debug("all data was sent to workers.")
  }

  // Executes a given command. If it fails, shutdown and respond to the sender with failure.
  private def executeScriptOrFail(script: String, failureReceiver: ActorRef) : Unit = {
    executeCmd(script) match {
      case Success(_) => // All good. We're done
      case Failure(exception) => // Throw exception of script
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
    // Make the file executable, if necessary
    val file = new java.io.File(cmd)
    if (file.isFile) file.setExecutable(true, false)
    log.info(s"""Executing: "$cmd" """)
    val processLogger = ProcessLogger(line => log.info(line))
    Try(cmd!(processLogger)) match {
      case Success(0) => Success(0)
      case Success(errorExitValue) => 
        Failure(new RuntimeException(s"Script exited with exit_value=$errorExitValue"))
      case Failure(ex) => Failure(ex)
    }
  }

  
  def executeSqlUpdateOrFail(sqlQuery: String, failureReceiver: ActorRef) {
    Try(dataStore.queryUpdate(sqlQuery)) match {
      case Success(_) =>
      case Failure(ex) =>
        failureReceiver ! Status.Failure(ex)
        context.stop(self)
        throw new RuntimeException(ex.toString)
    }
  }

  def executeSqlUpdate(sqlQuery: String) {
    dataStore.queryUpdate(sqlQuery)
  }

  def executeSqlQueryOrFail(query: String, failureReceiver: ActorRef, pipeOutFilePath: String = null) { 
    // dataStore.executeCmd(query)
    val file = File.createTempFile(s"exec_sql", ".sh")
    val writer = new PrintWriter(file)

    val pipeOutStr = pipeOutFilePath match {
      case null => ""
      case _ => " > " + pipeOutFilePath
    }
    val cmd =  sqlQueryPrefix + " -c " + "\"\"\"" + query + "\"\"\"" + pipeOutStr

    writer.println(s"${cmd}")
    writer.close()
    log.debug(s"Temporary bash file saved to ${file.getAbsolutePath()}")
    executeScriptOrFail(file.getAbsolutePath(), failureReceiver)
    // executeScriptOrFail(cmd, failureReceiver)
    
    // executeCmd(s"psql -d ${dbname} -c " + "\"\"\"" + query + "\"\"\"")
  }
  def executeSqlFileOrFail(filename: String, failureReceiver: ActorRef) { 
    val file = File.createTempFile(s"exec_sql", ".sh")
    val writer = new PrintWriter(file)
    val dbname = dbSettings.dbname
    val pguser = dbSettings.user
    val pgport = dbSettings.port
    val pghost = dbSettings.host
    // TODO do not use password for now
    val cmd = sqlQueryPrefix + " < " + filename
    writer.println(s"${cmd}")
    writer.close()
    log.debug(s"Temporary bash file saved to ${file.getAbsolutePath()}")
    executeScriptOrFail(file.getAbsolutePath(), failureReceiver)
    // executeCmd(s"psql -d ${dbname} < " + filename)
  }

}