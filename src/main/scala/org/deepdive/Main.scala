package org.deepdive

import com.typesafe.config.ConfigFactory
import org.deepdive.settings._
import scopt._
import java.io.File


/* DeepDive main entry point */
object Main extends App with Logging {

  // Save all files in a directory named by date
  val dateStr = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HHmmss").format(new java.util.Date())
  val defaultOutputDir = new java.io.File(s"./out/${dateStr}")

  // Parsing command-line options
  case class CliOptions(
      configFile: File = null,
      outputDir: File = defaultOutputDir)
  val parser = new scopt.OptionParser[CliOptions]("scopt") {
    head("deepdive", "0.7.0")
    opt[File]('c', "config") required() valueName("<config>") action { (x,c) =>
      c.copy(configFile = x)
    } text("configuration file path (required)")
    opt[File]('o', "output-dir") valueName("<outputDir>") action { (x,c) =>
      c.copy(outputDir = x)
    } text("Output directory for all files (calibration data, graph data)")
  }

  val options = parser.parse(args, CliOptions()).getOrElse(null)

  options match {
    case null =>
    case _ =>
      // Starting the pipeline
      log.info(s"Running pipeline with configuration from ${options.configFile.getAbsolutePath}")
      val userConfig = ConfigFactory.parseFile(options.configFile)
      val defaultConfig = ConfigFactory.load
      val resolvedConfig = userConfig.withFallback(defaultConfig).resolve()
      DeepDive.run(resolvedConfig, options.outputDir.getCanonicalPath)

  }

}
