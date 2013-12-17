package org.deepdive

import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config._
import java.io.File
import org.deepdive.context._
import org.deepdive.settings.{Settings}
import org.deepdive.datastore.{PostgresDataStore}
import org.deepdive.extraction.{ExtractionManager, ExtractionTask}
import org.deepdive.inference.{InferenceManager, FactorGraphBuilder, FileGraphWriter}
import org.deepdive.calibration._
import scala.concurrent.duration._
import scala.concurrent.{Future, Await}
import scala.sys.process._
import scala.util.Sorting

object Pipeline extends Logging {

  lazy val VARIABLES_DUMP_FILE = new File("target/variables.tsv")
  lazy val FACTORS_DUMP_FILE = new File("target/factors.tsv")
  lazy val WEIGHTS_DUMP_FILE = new File("target/weights.tsv")
  lazy val SAMPLING_OUTPUT_FILE = new File("target/inference_result.out")
  val NUM_SAMPLES = 1000
  val NUM_SAMPLING_THREADS = 4

  def run(config: Config) {

    // Get the actor system
    val system = Context.system

    // Load Settings
    Context.settings = Settings.loadFromConfig(config)    

    // Initialize the data store
    PostgresDataStore.init(Context.settings.connection.url, Context.settings.connection.user, 
      Context.settings.connection.password)

    // Start the Inference Manager
    val inferenceManager = system.actorOf(InferenceManager.props, "InferenceManager")

    // TODO: ETL Tasks: We ignore this for now

    // TODO: Have an extraction manager that manages and parallelizes the extractions
    // Start the ExtractorExecutor for each defined Extractor
    val extractionManager = system.actorOf(ExtractionManager.props, "ExtractionManager")
    // val extractorExecutor = system.actorOf(ExtractorExecutor.props(Settings.databaseUrl), "ExtractorExecutor")

    implicit val timeout = Timeout(30 minutes)
    implicit val ec = system.dispatcher
    
    // Run extractions
    log.info("Running extractors")
    val extractionResults = for {
      extractor <- Context.settings.extractors
      task = ExtractionTask(extractor)
      extractionResult <- Some(ask(extractionManager, ExtractionManager.AddTask(task)))
    } yield extractionResult
    Await.result(Future.sequence(extractionResults), 30 minutes)
    log.info("Extractors execution finished")

    // Build the factor graph
    log.info("Building factor graph")
    val graphResults = for {
      factor <- Context.settings.factors
      graphResult <- Some(ask(inferenceManager, FactorGraphBuilder.AddFactorsAndVariables(factor, 
        Context.settings.calibrationSettings.holdoutFraction)))
    } yield graphResult
    Await.result(Future.sequence(graphResults), 30 minutes)
    log.info("Successfully built factor graph")

    // Dump the factor graph to a file
    PostgresDataStore.withConnection { implicit conn =>
      FileGraphWriter.dump(VARIABLES_DUMP_FILE, FACTORS_DUMP_FILE, WEIGHTS_DUMP_FILE)
    }

    // Call the sampler executable
    log.info(s"Running gibbs sampler with num_samples=${NUM_SAMPLES} num_threads=${NUM_SAMPLING_THREADS}")
    val samplerOutput = Seq("java", "-jar", "lib/gibbs_sampling-assembly-0.1.jar", 
      "--variables", VARIABLES_DUMP_FILE.getCanonicalPath, 
      "--factors", FACTORS_DUMP_FILE.getCanonicalPath, 
      "--weights", WEIGHTS_DUMP_FILE.getCanonicalPath,
      "-n", NUM_SAMPLES.toString, "-t", NUM_SAMPLING_THREADS.toString, 
      "--output", SAMPLING_OUTPUT_FILE.getCanonicalPath).!!
    log.info(samplerOutput)
    log.info(s"Gibbs sampling finished, output in file=${SAMPLING_OUTPUT_FILE.getCanonicalPath}")

    // Write the inference result back to the database
    val inferenceWritebackResult = inferenceManager ? 
      InferenceManager.WriteInferenceResult(SAMPLING_OUTPUT_FILE.getCanonicalPath)
    Await.result(inferenceWritebackResult, 5.minutes)

    // Writer calibration data
    val calibrationWriter = new CalibrationDataWriter(new PostgresCalibrationData)
    calibrationWriter.writeBucketCounts("target/calibration_data/counts")
    calibrationWriter.writeBucketPrecision("target/calibration_data/precision")

    // Shut down actor system
    system.shutdown()
    system.awaitTermination()

  }

}