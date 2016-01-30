package org.deepdive.ddlog

// A command-line interface
object DeepDiveLog {

  object Mode extends Enumeration {
  type Mode = Value
  // Four modes of DDlog compilation:
  // ORIGINAL => Generate standard application.conf
  // INCREMENTAL => Generate incremental application.conf
  // MATERIALIZATION => Materialize existing factor graph for incremental mode
  // MERGE => Merge new generated data into original table
  val ORIGINAL, INCREMENTAL, MATERIALIZATION, MERGE  = Value

  }
  import Mode._

  type Program = List[Statement]
  type Query = (ConjunctiveQuery, List[Statement])

  case class Config
  (handler: DeepDiveLogHandler = null
   , inputFiles: List[String] = List()
   , query: String = null
   , mode: Mode = ORIGINAL
  )
  val commandLine = new scopt.OptionParser[Config]("ddlog") {
    val commonProgramOpts = List(
    )
    head("DDlog Compiler", "0.0.1")
    cmd("query")                       required() action { (_, c) => c.copy(handler = DeepDiveLogQueryCompiler)   } text("Compiles a SQL query to run against the program") children(
      arg[String]("QUERY")             required() action { (q, c) => c.copy(query = q) }                            text("DDLog query to compile against the program")
      )
    cmd("compile")                     required() action { (_, c) => c.copy(handler = DeepDiveLogCompiler)        } text("Compiles a deepdive.conf")
    cmd("print")                       required() action { (_, c) => c.copy(handler = DeepDiveLogPrettyPrinter)   } text("Prints given program after parsing")
    cmd("check")                       required() action { (_, c) => c.copy(handler = DeepDiveLogSemanticChecker) } text("Checks if given program is valid")
    cmd("export-schema")               required() action { (_, c) => c.copy(handler = DeepDiveLogSchemaExporter)  } text("Exports given program in JSON")
    opt[Unit]('i', "incremental")      optional() action { (_, c) => c.copy(mode    = INCREMENTAL)                } text("Whether to derive delta rules")
    opt[Unit]("materialization")       optional() action { (_, c) => c.copy(mode    = MATERIALIZATION)            } text("Whether to materialize origin data")
    opt[Unit]("merge")                 optional() action { (_, c) => c.copy(mode    = MERGE)                      } text("Whether to merge delta data")
    arg[String]("FILE...") minOccurs(0) unbounded() action { (f, c) => c.copy(inputFiles = c.inputFiles ++ List(f)) } text("Path to DDLog program files")
    checkConfig { c =>
      if (c.handler == null) failure("No command specified")
      else success
    }
  }

  def main(args: Array[String]) = {
    commandLine.parse(args, Config()) match {
      case Some(config) =>
        config.handler.run(config)
      case None =>
        System.err.println("[error] ")
    }
  }

}

// An abstraction of DeepDiveLog handlers
trait DeepDiveLogHandler {

  def run(program: DeepDiveLog.Program, config: DeepDiveLog.Config): Unit

  def run(config: DeepDiveLog.Config): Unit = try {
    // parse each file into a single program
    val parsedProgram = parseFiles(config.inputFiles)
    // run handler with the parsed program
    run(parsedProgram, config)
  } catch {
    case e: RuntimeException =>
      if (sys.env contains "DDLOG_STACK_TRACE") throw e
      else die(e.getMessage)
  }

  def parseFiles(fileNames: List[String]): DeepDiveLog.Program = {
    val ddlParser = new DeepDiveLogParser
    fileNames flatMap { ddlParser.parseProgramFile(_) }
  }

  def die(message: String = null) = {
    if (message != null)
      System.err.println("[error] " + message)
    System.exit(1)
  }
}
