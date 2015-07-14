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

  case class Config
  ( handler: DeepDiveLogHandler = null
  , inputFiles: List[String] = List()
  , mode: Mode = ORIGINAL
  )
  val parser = new scopt.OptionParser[Config]("ddlogc") {
    head("ddlogc", "0.0.1")
    cmd("compile")                     required() action { (_, c) => c.copy(handler = DeepDiveLogCompiler)        }
    cmd("print")                       required() action { (_, c) => c.copy(handler = DeepDiveLogPrettyPrinter)   }
    cmd("check")                       required() action { (_, c) => c.copy(handler = DeepDiveLogSemanticChecker) }
    opt[Unit]('i', "incremental")      optional() action { (_, c) => c.copy(mode    = INCREMENTAL)                } text("Whether to derive delta rules")
    opt[Unit]("materialization")       optional() action { (_, c) => c.copy(mode    = MATERIALIZATION)            } text("Whether to materialize origin data")
    opt[Unit]("merge")                 optional() action { (_, c) => c.copy(mode    = MERGE)                      } text("Whether to merge delta data")
    arg[String]("FILE...") unbounded() required() action { (f, c) => c.copy(inputFiles = c.inputFiles ++ List(f)) } text("Input DDLog programs files")
    checkConfig { c =>
      if (c.handler == null) failure("No command specified")
      else success
    }
  }

  def main(args: Array[String]) = {
    parser.parse(args, Config()) match {
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
    case e: RuntimeException => die(e.getMessage)
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
