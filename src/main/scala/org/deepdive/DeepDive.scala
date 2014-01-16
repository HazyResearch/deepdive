package org.deepdive

import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config._
import java.io.File
import org.deepdive.settings._
import org.deepdive.datastore.{JdbcDataStore}
import org.deepdive.extraction.{ExtractionManager, ExtractionTask, ExtractionTaskResult}
import org.deepdive.inference.{InferenceManager, FactorGraphBuilder}
import org.deepdive.profiling._
import org.deepdive.calibration._
import scala.concurrent.duration._
import scala.concurrent.{Future, Await}
import scala.util.{Try, Success, Failure}

object DeepDive extends Logging {

  lazy val VARIABLES_DUMP_FILE = new File("target/variables.tsv")
  lazy val FACTORS_DUMP_FILE = new File("target/factors.tsv")
  lazy val WEIGHTS_DUMP_FILE = new File("target/weights.tsv")
  lazy val SAMPLING_OUTPUT_FILE = new File("target/inference_result.out")

  def run(config: Config) {

    // Get the actor system
    val system = Context.system

    // Load Settings
    val settings = Settings.loadFromConfig(config)

    // Setup the data store
    JdbcDataStore.init(config)

    implicit val timeout = Timeout(1337 hours)
    implicit val ec = system.dispatcher

    // Start actors
    val profiler = system.actorOf(Profiler.props, "profiler")
    val taskManager = system.actorOf(TaskManager.props, "taskManager")
    val inferenceManager = system.actorOf(InferenceManager.props(
      taskManager, settings.schemaSettings.variables), "inferenceManager")
    val extractionManager = system.actorOf(
      ExtractionManager.props(settings.extractionSettings.parallelism), 
      "extractionManager")
    
    // Build tasks for extractors
    val extractionTasks = for {
      extractor <- settings.extractionSettings.extractors
      extractionTask = ExtractionTask(extractor)
    } yield Task(s"${extractor.name}", extractor.dependencies.toList, 
      extractionTask, extractionManager)

    // Build task to construct the factor graph
    val factorTasks = for {
      factor <- settings.inferenceSettings.factors
      factorTask = InferenceManager.FactorTask(factor, 
        settings.calibrationSettings.holdoutFraction, settings.inferenceSettings.insertBatchSize)
      // TODO: We don't actually neeed to wait for all extractions to finish. For now it's fine.
      taskDeps = extractionTasks.map(_.id)
    } yield Task(factor.name, taskDeps, factorTask, inferenceManager)

    val inferenceTask = Task("inference", factorTasks.map(_.id) ++ extractionTasks.map(_.id),
      InferenceManager.RunInference(settings.samplerSettings.javaArgs, 
        settings.samplerSettings.samplerArgs), inferenceManager)

    val calibrationTask = Task("calibration", List("inference"), 
      InferenceManager.WriteCalibrationData, inferenceManager)
    
    val reportingTask = Task("report", List("calibration"), Profiler.PrintReports, profiler, false)

    val terminationTask = Task("shutdown", List("report"), TaskManager.Shutdown, taskManager, false)

    val allTasks = extractionTasks ++ factorTasks ++ 
      List(inferenceTask, calibrationTask, reportingTask, terminationTask) 

    // Create a default pipeline that executes all tasks
    val defaultPipeline = Pipeline("_default", allTasks.map(_.id).toSet)

    // Figure out which pipeline to run
    val activePipeline = settings.pipelineSettings.activePipeline match {
      case Some(pipeline) => pipeline.copy(tasks = pipeline.tasks ++ 
        Set("inference", "calibration", "report", "shutdown"))
      case None => defaultPipeline
    }

    // We remove all tasks dependencies that are not in the pipeline
    val filteredTasks = allTasks.filter(t => activePipeline.tasks.contains(t.id)).map { task =>
      val newDependencies = task.dependencies.filter(activePipeline.tasks.contains(_))
      task.copy(dependencies=newDependencies)
    }

    log.info(s"Running pipeline=${activePipeline.id} with tasks=${filteredTasks.map(_.id)}")

    // Schedule all Tasks. 
    for (task <- filteredTasks) {
      taskManager ! TaskManager.AddTask(task)
    }

    // Wait for the system to shutdown
    system.awaitTermination()

    // Clean up resources
    Context.shutdown()
  }

}
