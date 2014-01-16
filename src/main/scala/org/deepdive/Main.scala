package org.deepdive

import com.typesafe.config.ConfigFactory
import org.deepdive.settings._
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

  val options = parser.parse(args, CliOptions(null)).get

  // Starting the pipeline
  log.info(s"Running pipeline with configuration from ${options.configFile.getAbsolutePath}")
  val userConfig = ConfigFactory.parseFile(options.configFile)
  val defaultConfig = ConfigFactory.load
  val resolvedConfig = userConfig.withFallback(defaultConfig).resolve()
  DeepDive.run(resolvedConfig)

}