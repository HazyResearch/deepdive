package org.deepdive

import akka.event.Logging
import com.typesafe.config.ConfigFactory
import org.deepdive.context.Context
import org.deepdive.inference.InferenceManager
import org.deepdive.context.Settings
import org.deepdive.extraction.{ExtractorExecutor, ExtractionTask}
import scopt._
import java.io.File


/* DeepDive main entry point */
object Main extends App {
  
  val log = Logging.getLogger(Context.system, this)

  // Parsing command-line options
  case class CliOptions(configFile: File)
  val parser = new scopt.OptionParser[CliOptions]("scopt") {
    head("deepdive", "0.1")
    opt[File]('c', "config") required() valueName("<config>") action { (x,c) =>
      c.copy(configFile = x)
    } text("the configuration file path (required)")
  }

  parser.parse(args, CliOptions(null)) map { c =>
    Context.configFile = c.configFile
  } getOrElse {
    // Option parsing failed.
    System.exit(1)
  }

  log.info(s"Running the pipeline with configuration from ${Context.configFile.getAbsolutePath}")
  val userConfig = ConfigFactory.parseFile(Context.configFile)
  val defaultConfig = ConfigFactory.load
  Pipeline.run(userConfig.withFallback(defaultConfig))

}