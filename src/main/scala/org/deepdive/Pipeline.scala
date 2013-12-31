package org.deepdive

import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config._
import java.io.File
import org.deepdive.settings.{Settings}
import org.deepdive.datastore.{PostgresDataStore}
import org.deepdive.extraction.{ExtractionManager, ExtractionTask, ExtractionTaskResult}
import org.deepdive.inference.{InferenceManager, FactorGraphBuilder, FactorTask}
import org.deepdive.profiling._
import org.deepdive.calibration._
import scala.concurrent.duration._
import scala.concurrent.{Future, Await}
import scala.util.{Try, Success, Failure}

object Pipeline extends Logging {

  lazy val VARIABLES_DUMP_FILE = new File("target/variables.tsv")
  lazy val FACTORS_DUMP_FILE = new File("target/factors.tsv")
  lazy val WEIGHTS_DUMP_FILE = new File("target/weights.tsv")
  lazy val SAMPLING_OUTPUT_FILE = new File("target/inference_result.out")

  def run(config: Config) {

    // Get the actor system
    val system = Context.system

    // Load Settings
    val settings = Settings.loadFromConfig(config)    

    // Initialize the data store
    PostgresDataStore.init(settings.connection.url, settings.connection.user, 
      settings.connection.password)

    implicit val timeout = Timeout(1337 hours)
    implicit val ec = system.dispatcher

    // Start actors
    val profiler = system.actorOf(Profiler.props, "profiler")
    val taskManager = system.actorOf(TaskManager.props, "taskManager")
    val inferenceManager = system.actorOf(InferenceManager.props(
      taskManager, settings.schemaSettings.variables), "inferenceManager")
    // TODO Configuration setting for parallelism. Right now we execute extractors sequentially
    val extractionManager = system.actorOf(ExtractionManager.props(1), "extractionManager")
    
    // Build tasks for extractors
    val extractionTasks = for {
      extractor <- settings.extractionSettings.extractors
      extractionTask = ExtractionTask(extractor)
    } yield Task(s"${extractor.name}", extractor.dependencies.toList, 
      extractionTask, extractionManager)

    // Build task to construct the factor graph
    val factorTasks = for {
      factor <- settings.factors
      factorTask = FactorTask(factor, settings.calibrationSettings.holdoutFraction)
      // TODO: We don't actually neeed to wait for all extractions to finish. For now it's fine.
      taskDeps = extractionTasks.map(_.id)
    } yield Task(factor.name, taskDeps, factorTask, inferenceManager)

    val inferenceTask = Task("inference", factorTasks.map(_.id),
      InferenceManager.RunInference(settings.samplerSettings.javaArgs, 
        settings.samplerSettings.samplerArgs), inferenceManager)

    val calibrationTask = Task("calibration_plots", List("inference"), 
      InferenceManager.WriteCalibrationData, inferenceManager)
    
    val reportingTask = Task("report", List("calibration_plots"), Profiler.PrintReports, profiler)

    val terminationTask = Task("shutdown", List("report"), "shutdown", taskManager)

    val allTasks = extractionTasks ++ factorTasks ++ 
      List(inferenceTask, calibrationTask, reportingTask, terminationTask) 

    // Schedule all Tasks. 
    allTasks.foreach( task => taskManager ! TaskManager.AddTask(task) )

    // Wait for the system to shutdown
    system.awaitTermination()

    // Clean up resources
    Context.shutdown()
  }

}
