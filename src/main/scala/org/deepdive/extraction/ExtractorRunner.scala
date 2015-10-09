package org.deepdive.extraction

import akka.actor._
import akka.routing._
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import org.deepdive.settings._
import org.deepdive.Context
import org.deepdive.extraction._
import org.deepdive.extraction.ExtractorRunner._
import org.deepdive.datastore._
import org.deepdive.datastore.FileDataUtils
import org.deepdive.Logging
import org.deepdive.datastore.DataLoader
import org.deepdive.inference.InferenceNamespace
import scala.util.{Try, Success, Failure}
import scala.concurrent._
import scala.concurrent.duration._
import scala.sys.process._
import collection.mutable.HashMap
import rx.lang.scala.subjects._
import play.api.libs.json._
import scala.util.Random
import java.io.{File, PrintWriter}
import java.net.InetAddress
import scala.io.Source
import org.deepdive.helpers.Helpers
import org.deepdive.helpers.Helpers.{Mysql, Psql}

/**
 *  Companion object to the ExtractorRunner, using a state machine model.
 *  Only change states for JSON extractor. For other extractors, do all the work in "Idle" state.
 */
object ExtractorRunner {

  def props(dataStore: JdbcDataStore, dbSettings: DbSettings) = Props(classOf[ExtractorRunner], dataStore, dbSettings)


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
class ExtractorRunner(dataStore: JdbcDataStore, dbSettings: DbSettings) extends Actor
  with ActorLogging with FSM[State, Data] {

  import ExtractorRunner._
  // Execute futures using the current Akka dispatcher
  import context.dispatcher
  implicit val timeout = Timeout(1337.hours)

  private val PRINT_PERIOD = 30.seconds

  // UDF dir path -> staged env path on the DB server
  private val piggyEnvs = HashMap[String, String]()

  // Branch by database driver type (temporary solution)
  val dbtype = Helpers.getDbType(dbSettings)
  val sqlQueryPrefix = dbtype match {
    case Psql => "psql " + Helpers.getOptionString(dbSettings)
    case Mysql => "mysql " + Helpers.getOptionString(dbSettings)
  }

  val sqlAnalyzeCommand = dbtype match {
    case Psql => "ANALYZE "
    case Mysql => "ANALYZE TABLE "
  }

    // DONE mysql pw: -p=password. psql: cannot?

  // Properties to start workers
  def workerProps = ProcessExecutor.props

  // Periodically print the status
  val scheduledStatus = context.system.scheduler.schedule(PRINT_PERIOD, PRINT_PERIOD, self, PrintStatus)

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
          failTask(s"${task.extractor.style} no longer supported.", taskSender)
          runAfterScript(task, taskSender)
          // TODO support json_extractor again using `deepdive-sql eval ... format=json`

        // TSV extractor: Get rid of scala file operations
        // COPY to a file, split files, and send to extractors
        case "tsv_extractor" =>
          runTsvExtractor(task, dbSettings, taskSender)
          runAfterScript(task, taskSender)

        // Execute the sql query from sql extractor
        case "sql_extractor" =>
          log.debug("Executing SQL query: " + task.extractor.sqlQuery)
          executeSqlUpdateOrFail(task.extractor.sqlQuery, taskSender)
          runAfterScript(task, taskSender)

        case "cmd_extractor" =>
          task.extractor.cmd.foreach {
            cmd => executeScriptOrFail(cmd, taskSender)
          }
          runAfterScript(task, taskSender)

        case "plpy_extractor" =>
          runPlpyExtractor(task, dbSettings, taskSender)
          runAfterScript(task, taskSender)

        case "piggy_extractor" =>
          runPiggyExtractor(task, dbSettings, taskSender)
          runAfterScript(task, taskSender)
      }

  }

  // This state can only happen for JSON extractors.
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
          executeSqlUpdateOrFail(s"${sqlAnalyzeCommand} ${task.extractor.outputRelation};", taskSender)
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


  /**
   *  Executes a given command. If it fails, shutdown and respond to the sender with failure.
   */
  private def executeScriptOrFail(script: String, failureReceiver: ActorRef) : Unit = {
    try {
      Helpers.executeCmd(script);
    } catch {
      case e: Throwable =>
        log.error(e.toString)
        failureReceiver ! Status.Failure(e)
        context.stop(self)
        throw new RuntimeException(e.toString)
    }
  }

  /**
   * Now do not allow mysql to talk to datastore component. Use executeSqlQueryOrFail instead.
   */
  def executeSqlUpdateOrFail(sqlQuery: String, failureReceiver: ActorRef) {
    dbtype match {
      case Psql =>
        Try(dataStore.queryUpdate(sqlQuery)) match {
          case Success(_) =>
          case Failure(ex) =>
            failureReceiver ! Status.Failure(ex)
            context.stop(self)
            throw new RuntimeException(ex.toString)
        }
      case Mysql =>
        executeSqlQueryOrFail(sqlQuery, failureReceiver)
    }

  }

  /**
   * Executes a SQL query by piping it into a file without talking to JDBC.
   */
  def executeSqlQueryOrFail(query: String, failureReceiver: ActorRef, pipeOutFilePath: String = null) {
    try {
      Helpers.executeSqlQueriesByFile(dbSettings, query, pipeOutFilePath)
    } catch {
      case e: Throwable =>
        log.error(e.toString)
        failureReceiver ! Status.Failure(e)
        context.stop(self)
        throw new RuntimeException(e.toString)
    }
  }

  /**
   * This function is only used by plpy extractor when the function to execute is compiled.
   */
  private def executeSqlFileOrFail(filename: String, failureReceiver: ActorRef) {
    // val queryOutputPath = Context.outputDir + s"/tmp/"
    // val file = new File(queryOutputPath + s"exec_sql.sh")
    val file = File.createTempFile(s"exec_sql", ".sh")
    val writer = new PrintWriter(file)

    // TODO do not use password for now
    val cmd = sqlQueryPrefix + " < " + filename
    writer.println(s"${cmd}")
    writer.close()
    log.debug(s"Temporary bash file saved to ${file.getAbsolutePath()}")
    executeScriptOrFail(file.getAbsolutePath(), failureReceiver)
  }

  /**
   * Fail the current task, log the error message, and throw new RuntimeException.
   * This will terminate DeepDive.
   * @throws RuntimeException
   */
  private def failTask(message: String, failureReceiver: ActorRef) {
    log.error(message)
    val exception = new RuntimeException(message)
    failureReceiver ! Status.Failure(exception)
    context.stop(self)
    throw new RuntimeException(message)
  }

  /**
   * Run UDF of TSV extractor. Do not include before and after script
   */
  private def runTsvExtractor(task: ExtractionTask, dbSettings: DbSettings, taskSender: ActorRef) = {

    log.debug(s"Parallel Loading: ${dbSettings.gpload}")
    val dl = new DataLoader
    val parallelLoading = dbSettings.gpload

    val udfCmd = task.extractor.udf
    // make udfCmd executable if file
    val udfFile = new java.io.File(udfCmd)
    if (udfFile.isFile)
      udfFile.setExecutable(true, false)
    val funcName = s"func_${task.extractor.name}"

    val inputQuery = task.extractor.inputQuery match {
      case DatastoreInputQuery(query) => query
      case _ =>
    }

    val outputRel = task.extractor.outputRelation

    // TODO do not use password for now

    val queryOutputPath = Context.outputDir + s"/tmp/"
    log.info(queryOutputPath)
    // Try to create the extractor output directory if not already present
    val queryOutputPathDir = new File(queryOutputPath)
    if ((!queryOutputPathDir.exists()) && (!queryOutputPathDir.mkdirs())) {
      Status.Failure(new RuntimeException(s"TSV extractor directory creation failed"))
    }

    // NEW: for mysqlimport compatibility, the file basename must be same as table name.
    val queryOutputFile = new File(queryOutputPath + s"${outputRel}.copy_query_${funcName}.tsv")
    val gpFileName = s"${outputRel}_query_unload"
    val psqlFilePath = queryOutputFile.getAbsolutePath()

    // Get the actual dumped file
    // val fname = queryOutputFile.getName()
    val fname = parallelLoading match {
      case true => gpFileName
      case _ => queryOutputFile.getName()
    }

    val fpath = parallelLoading match {
      case true => dbSettings.gppath
      case _ => queryOutputFile.getParent()
    }

    // Clean the output path first
    val delCmd = s"find ${fpath} -name '${fname}*' 2>/dev/null -print0 | xargs -0 rm -f"
    log.info(s"Executing: ${delCmd}")
    val delTmpFile = new File(queryOutputPath + s"exec_delete.sh")
    // val delTmpFile = File.createTempFile(s"exec_delete", ".sh")
    val delWriter = new PrintWriter(delTmpFile)
    delWriter.println(s"${delCmd}")
    delWriter.close()
    executeScriptOrFail(delTmpFile.getAbsolutePath(), taskSender)

    // Helpers.executeCmd(delCmd) // This won't work because of escaping issues?

    try {
      dl.unload(fname, psqlFilePath, dbSettings, s"${inputQuery}", "")
    } catch {
      case exception: Throwable =>
        log.error(exception.toString)
        taskSender ! Status.Failure(exception)
        context.stop(self)
        throw exception
    }

    // Get the actually dumped file path
    val actualDumpedPath = s"${fpath}/${fname}"
    log.info(s"File dumped to ${actualDumpedPath}")
    val splitPrefix = s"${actualDumpedPath}-"
    val linesPerSplit = task.extractor.inputBatchSize
    val splitCmd = s"split -a 10 -l ${linesPerSplit} " + actualDumpedPath + s" ${splitPrefix}"

    log.info(s"Executing split command...")
    executeScriptOrFail(splitCmd, taskSender)

    val maxParallel = task.extractor.parallelism

    // Note (msushkov): the extractor must take TSV as input and produce TSV as output
    val runCmd = s"touch '${fpath}/${fname}-'; " + // XXX make sure xargs gets at least one file to process
        s"find ${fpath} -name '${fname}-*' 2>/dev/null -print0 | xargs -0 -P ${maxParallel} -L 1 bash -c '${udfCmd} " + "<" + " \"$0\" > \"$0.out\"'"

    log.info(s"Executing parallel UDF command: ${runCmd}")
    // executeScriptOrFail(runCmd, taskSender)

    val udfTmpFile = new File(queryOutputPath + s"exec_parallel_udf.sh")
    // val udfTmpFile = File.createTempFile(s"exec_parallel_udf", ".sh")
    val writer = new PrintWriter(udfTmpFile)
    writer.println(s"${runCmd}")
    writer.close()
    log.debug(s"Temporary UDF file saved to ${udfTmpFile.getAbsolutePath()}")
    executeScriptOrFail(udfTmpFile.getAbsolutePath(), taskSender)

    // Copy each of the files into the DB. If user is using Greenplum, use gpload (TODO)

    // If loader specified, use the chosen loader

    task.extractor.loader match {
      case "ndbloader" =>
        if (dbtype != Mysql) {
          throw new RuntimeException("ERROR: ndbloader can only be used on MySQL cluster.")
        }
        val loaderConfig = task.extractor.loaderConfig
        try {
          dl.ndbLoad(
            fpath,                  // fileDirPath: String,
            s"${fname}-*.out",      // fileNamePattern: String,
            dbSettings,             // dbSettings: DbSettings,
            loaderConfig.schemaFile,// schemaFilePath: String,
            loaderConfig.connection,// ndbConnectionString: String,
            loaderConfig.threads,   // threadNum: Integer,
            loaderConfig.parallelTransactions // parallelTransactionNum: Integer
            )
        } catch {
          case exception: Throwable =>
            log.error(exception.toString)
            taskSender ! Status.Failure(exception)
            context.stop(self)
            throw exception
        }

      case _ =>
        // If parallelWriteback is specified, run the loader with the GP loader.
        dl.load(s"${fpath}/${fname}-*.out", outputRel, dbSettings)

    }

    log.info("Analyzing output relation.")
    executeSqlUpdateOrFail(s"${sqlAnalyzeCommand} ${outputRel};", taskSender)

    log.info("Removing temporary files...")
    queryOutputFile.delete()

    executeScriptOrFail(delTmpFile.getAbsolutePath(), taskSender)
    delTmpFile.delete()
    queryOutputPathDir.delete()

  }

  /**
   * Run PLPY extractor
   */
  private def runPlpyExtractor(task: ExtractionTask, dbSettings: DbSettings, taskSender: ActorRef) = {
    if (dbtype != Psql) {
      failTask(s"do not support ${task.extractor.style} on ${dbtype}.", taskSender)
    }

    // Create Function in GP
    val udfFile = task.extractor.udf
    val deepDiveDir = Context.deepdiveHome
    val funcName = s"func_${task.extractor.name}"
    val sqlFunctionFile = File.createTempFile(funcName, ".sql")

    executeScriptOrFail(s"ddext.py ${udfFile} ${sqlFunctionFile} ${funcName}", taskSender)
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

    val sqlInsertFile = File.createTempFile(s"${funcName}_exec", ".sql")
    executeScriptOrFail(s"ddext_input_sql_translator.py ${udfFile} ${inputQueryFile} ${outputRel} ${funcName} ${sqlInsertFile}", taskSender)

    log.debug(s"Compiled query into: ${sqlInsertFile}")

    // Execute query in parallel in GP
    executeSqlFileOrFail(sqlInsertFile.getAbsolutePath(), taskSender)
    log.info(s"Finish executing UDF in database!")

    log.debug("Analyzing output relation.")
    executeSqlUpdateOrFail(s"${sqlAnalyzeCommand} ${outputRel};", taskSender)
  }

  /**
   * Run piggy extractor
   */
  private def runPiggyExtractor(task: ExtractionTask, dbSettings: DbSettings, taskSender: ActorRef) = {
    if (dbtype != Psql) {
      failTask(s"do not support ${task.extractor.style} on ${dbtype}.", taskSender)
    }

    if (!dataStore.existsLanguage("plpythonu")) {
      // Piggy extractor requires plpythonu,
      // before execution, check whether the language exists.
      // if not, install it.
      dataStore.executeSqlQueries("CREATE LANGUAGE plpythonu;")
    }

    if (!dataStore.existsFunction("piggy_setup_package")) {
      // Piggy extractor needs to run SQL query to setup
      // the function `piggy_setup_package`. Therefore,
      // if such a function does not exist in the current
      // database, install it.
      dataStore.executeSqlQueries(SQLFunctions.piggyExtractorDriverDeclaration, false)
    }

    // Upload UDF directory and setup runtime env on DB nodes
    val udfDir = task.extractor.udfDir
    if (!new File(udfDir).exists()) {
      throw new RuntimeException("UDF directory does not exist: " + udfDir)
    }

    // Setup UDF env
    val envDir = ensurePiggyEnv(udfDir)

    val udf = task.extractor.udf
    val outputRel = task.extractor.outputRelation
    val inputQuery = task.extractor.inputQuery match {
      case DatastoreInputQuery(query) => query
      case _ => ""
    }

    val deepDiveDir = Context.deepdiveHome
    val params = Json.obj(
      "dir" -> envDir,
      "script" -> udf,
      "source" -> inputQuery,
      "target" -> task.extractor.outputRelation,
      "is_pgxl" -> dataStore.isUsingPostgresXL
    )
    val paramsJson = Json.stringify(params)
    val cmd = Seq("piggy_prepare.py", paramsJson)
    val res = cmd.!!
    val queries = Json.parse(res)

    // Run plpython UDF
    val create_tables: String = (queries \ "sql_create_tables").as[String]
    val create_functions: String = (queries \ "sql_create_functions").as[String]
    val insertion: String = (queries \ "sql_insert").as[String]
    val getlog: String = (queries \ "sql_getlog").as[String]
    val cleaning: String = (queries \ "sql_clean").as[String]

    dataStore.executeSqlQueries(create_tables, false)
    dataStore.executeSqlQueries(create_functions, false)

    class GetLogThread extends Runnable {
      var stopped = false
      val conn = dataStore.borrowConnection()

      def getLog(last_check: Boolean) {
        this.synchronized {
          val ps = conn.prepareStatement(getlog)
          ps.setBoolean(1, last_check)
          val rs = ps.executeQuery()
          while (rs.next()) {
            val content = rs.getString(1)
            if (content != null) {
              content.split("\n").foreach { line =>
                log.info(line)
              }
            }
          }
          rs.close()
        }
      }

      def die() {
        stopped = true
        getLog(true)
        conn.close()
      }

      def run() {
        while (!stopped) {
          getLog(false)
          Thread.sleep(1000)
        }
      }
    }

    val thread = new GetLogThread
    try {
      log.info(insertion)
      dataStore.prepareStatement(insertion) { ps =>
        new Thread(thread).start()
        ps.executeUpdate()
      }
    } finally {
      thread.die()
      log.debug(cleaning)
      dataStore.executeSqlQueries(cleaning, false)
    }

    log.debug("Analyzing output relation.")
    executeSqlUpdateOrFail(s"${sqlAnalyzeCommand} ${outputRel};", taskSender)
  }


  private def ensurePiggyEnv(udfDir: String): String = {
    if (piggyEnvs.contains(udfDir)) {
      return piggyEnvs(udfDir)
    }
    val blob = FileDataUtils.zipDir(udfDir)
    val localhost = InetAddress.getLocalHost
    val hostname = localhost.getHostName
    val pkgname = Helpers.slugify(hostname) + '_' + Helpers.md5Hash(localhost.toString + udfDir)
    var sql = "SELECT piggy_setup_package(?, ?)"
    if (dataStore.isUsingPostgresXL) {
      sql += " FROM pgxl_dual_hosts"
    }
    var remotePath: String = null
    dataStore.prepareStatement(sql) { ps =>
      ps.setString(1, pkgname)
      ps.setBytes(2, blob)
      val rs = ps.executeQuery()
      if (rs.next()) {
        remotePath = rs.getString(1)
        piggyEnvs(udfDir) = remotePath
        log.info("Piggy env source: " + udfDir)
        log.info("Piggy env destination: " + remotePath)
      }
      // For PGXL, we assume the TEMP DIR is the same for all data nodes.
      rs.close()
    }
    if (remotePath == null) {
      throw new RuntimeException("Failed to stage piggy UDF: " + udfDir)
    }
    return remotePath
  }


  /**
   * Run after script and finalize the extractor. Fail if the after script fails.
   */
  private def runAfterScript(task: ExtractionTask, taskSender: ActorRef) = {
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
