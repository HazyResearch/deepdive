package org.deepdive

import com.typesafe.config.ConfigFactory
import org.deepdive.settings._
import org.deepdive.inference.InferenceManager
import org.deepdive.extraction.{ExtractorExecutor, ExtractionTask}
import scopt._
import java.io.File


/* DeepDive main entry point */
object Main extends App with Logging {
  
  // Parsing command-line options
  case class CliOptions(configFile: File)
  val parser = new scopt.OptionParser[CliOptions]("scopt") {
    head("deepdive", "0.1")
    opt[File]('c', "config") required() valueName("<config>") action { (x,c) =>
      c.copy(configFile = x)
    } text("configuration file path (required)")
  }

  parser.parse(args, CliOptions(null)) map { c =>
    Context.configFile = c.configFile
  } getOrElse {
    // Option parsing failed.
    System.exit(1)
  }

  // Starting the pipeline
  log.info(s"Running pipeline with configuration from ${Context.configFile.getAbsolutePath}")
  val userConfig = ConfigFactory.parseFile(Context.configFile)
  val defaultConfig = ConfigFactory.load
  Pipeline.run(userConfig.withFallback(defaultConfig))

}