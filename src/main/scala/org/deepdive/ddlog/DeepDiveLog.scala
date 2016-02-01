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
  (handler: DeepDiveLogHandler = null
   , inputFiles: List[String] = List()
   , mode: Mode = ORIGINAL
   , skipDesugar: Boolean = false
  )
  val parser = new scopt.OptionParser[Config]("ddlog") {
    head("DDlog Compiler", "0.0.1")
    cmd("compile")                     required() action { (_, c) => c.copy(handler = DeepDiveLogCompiler)        }
    cmd("print")                       required() action { (_, c) => c.copy(handler = DeepDiveLogPrettyPrinter)   }
    cmd("check")                       required() action { (_, c) => c.copy(handler = DeepDiveLogSemanticChecker) }
    cmd("export-schema")               required() action { (_, c) => c.copy(handler = DeepDiveLogSchemaExporter)  }
    opt[Unit]('i', "incremental")      optional() action { (_, c) => c.copy(mode    = INCREMENTAL)                } text("Whether to derive delta rules")
    opt[Unit]("materialization")       optional() action { (_, c) => c.copy(mode    = MATERIALIZATION)            } text("Whether to materialize origin data")
    opt[Unit]("merge")                 optional() action { (_, c) => c.copy(mode    = MERGE)                      } text("Whether to merge delta data")
    opt[Unit]("skip-desugar")          optional() action { (_, c) => c.copy(skipDesugar = true)                   } text("Whether to skip desugaring and assume no sugar")
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

    // desugar unless explicitly said to skip so
    val programToRun =
      if (config.skipDesugar) parsedProgram
      else DeepDiveLogDesugarRewriter.derive(parsedProgram)

    // run handler with the parsed program
    run(programToRun, config)
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
