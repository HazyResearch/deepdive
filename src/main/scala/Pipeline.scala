package org.deepdive

import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config._
import java.io.File
import org.deepdive.settings.{Settings}
import org.deepdive.datastore.{PostgresDataStore}
import org.deepdive.extraction.{ExtractionManager, ExtractionTask, ExtractionTaskResult}
import org.deepdive.inference.{InferenceManager, FactorGraphBuilder}
import org.deepdive.profiling._
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

    // Start the profiler
    val profiler = system.actorOf(Profiler.props, "profiler")

    // Start the Inference and Extractions managers
    val inferenceManager = system.actorOf(InferenceManager.props, "InferenceManager")
    val extractionManager = system.actorOf(ExtractionManager.props, "ExtractionManager")
    // val extractorExecutor = system.actorOf(ExtractorExecutor.props(Settings.databaseUrl), "ExtractorExecutor")

    implicit val timeout = Timeout(3000 minutes)
    implicit val ec = system.dispatcher
    
    // Run extractions
    log.info("Running extractors")
    val extractionResults = for {
      extractor <- Context.settings.extractionSettings.extractors
      task = ExtractionTask(extractor)
      extractionResult <- Some(ask(extractionManager, ExtractionManager.AddTask(task)))
    } yield extractionResult.mapTo[ExtractionTaskResult]
    val results = Await.result(Future.sequence(extractionResults), 3000 minutes)
    
    // Shut down if an extract failed.
    if (results.exists(!_.success)) {
      Context.shutdown(1)
      return
    }
    
    log.info("Extractors execution finished")

    // Build the factor graph
    log.info("Building factor graph")
    val graphResults = for {
      factor <- Context.settings.factors
      graphResult <- Some(ask(inferenceManager, FactorGraphBuilder.AddFactorsAndVariables(factor, 
        Context.settings.calibrationSettings.holdoutFraction)))
    } yield graphResult
    Await.result(Future.sequence(graphResults), 3000 minutes)
    log.info("Successfully built factor graph")

    // Dump the factor graph to a file
    val dumpResult = inferenceManager ? InferenceManager.DumpFactorGraph(VARIABLES_DUMP_FILE.getCanonicalPath, 
      FACTORS_DUMP_FILE.getCanonicalPath, WEIGHTS_DUMP_FILE.getCanonicalPath)
    Await.result(dumpResult, 3000 minutes)

    log.debug(s"largest_variable_id=${PostgresDataStore.currentId}")

    // Call the sampler executable
    val samplingStartTime = System.currentTimeMillis
    log.info(s"Running gibbs sampler with num_samples=${NUM_SAMPLES} num_threads=${NUM_SAMPLING_THREADS}")
    val samplerCmd = Seq("java", Context.settings.samplerSettings.javaArgs, 
      "-jar", "lib/gibbs_sampling-assembly-0.1.jar", 
      "--variables", VARIABLES_DUMP_FILE.getCanonicalPath, 
      "--factors", FACTORS_DUMP_FILE.getCanonicalPath, 
      "--weights", WEIGHTS_DUMP_FILE.getCanonicalPath,
      "--output", SAMPLING_OUTPUT_FILE.getCanonicalPath) ++ Context.settings.samplerSettings.samplerArgs.split(" ")
    log.info(s"""Executing: ${samplerCmd.mkString(" ")}""")
    val samplerOutput = samplerCmd.!!
    log.info(samplerOutput)
    log.info(s"Gibbs sampling finished, output in file=${SAMPLING_OUTPUT_FILE.getCanonicalPath}")
    val samplingEndTime = System.currentTimeMillis

    profiler ! Profiler.SamplerFinished(samplingStartTime, samplingEndTime)

    // Write the inference result back to the database
    val inferenceWritebackResult = inferenceManager ? 
      InferenceManager.WriteInferenceResult(SAMPLING_OUTPUT_FILE.getCanonicalPath)
    Await.result(inferenceWritebackResult, 500.minutes)

    // Writer calibration data
    val calibrationWritebackResult = inferenceManager ? InferenceManager.WriteCalibrationData(
      "target/calibration_data/counts", "target/calibration_data/precision")
    Await.result(calibrationWritebackResult, 500.minutes)

    // Shut down actor system
    Context.shutdown()
  }

}
