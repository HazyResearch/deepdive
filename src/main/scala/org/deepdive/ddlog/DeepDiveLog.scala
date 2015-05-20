package org.deepdive.ddlog

// A command-line interface
object DeepDiveLog {
  type Program = List[Statement]

  case class Config
  ( handler: DeepDiveLogHandler = null
  , inputFiles: List[String] = List()
  , isIncremental: Boolean = false
  )
  val parser = new scopt.OptionParser[Config]("ddlogc") {
    head("ddlogc", "0.0.1")
    cmd("compile")                     required() action { (_, c) => c.copy(handler = DeepDiveLogCompiler)        }
    cmd("print")                       required() action { (_, c) => c.copy(handler = DeepDiveLogPrettyPrinter)   }
    opt[Unit]('i', "incremental")      optional() action { (_, c) => c.copy(isIncremental = true)                 } text("Whether to derive delta rules")
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
