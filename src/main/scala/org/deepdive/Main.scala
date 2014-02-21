package org.deepdive

import com.typesafe.config.ConfigFactory
import org.deepdive.settings._
import scopt._
import java.io.File


/* DeepDive main entry point */
object Main extends App with Logging {
  
  // Parsing command-line options
  case class CliOptions(configFile: File, outputDir: File)
  val parser = new scopt.OptionParser[CliOptions]("scopt") {
    head("deepdive", "0.1")
    opt[File]('c', "config") required() valueName("<config>") action { (x,c) =>
      c.copy(configFile = x)
    } text("configuration file path (required)")
  }

  // Save all files in a directory named by date
  val dateStr = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HHmmss").format(new java.util.Date())
  val outputDir = new java.io.File(s"./out/${dateStr}")

  val options = parser.parse(args, CliOptions(null, outputDir)).get

  // Starting the pipeline
  log.info(s"Running pipeline with configuration from ${options.configFile.getAbsolutePath}")
  val userConfig = ConfigFactory.parseFile(options.configFile)
  val defaultConfig = ConfigFactory.load
  val resolvedConfig = userConfig.withFallback(defaultConfig).resolve()
  DeepDive.run(resolvedConfig, options.outputDir.getCanonicalPath)

}