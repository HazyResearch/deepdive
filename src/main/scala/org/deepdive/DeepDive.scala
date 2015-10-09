package org.deepdive

import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config._
import java.io.File
import org.deepdive.settings._
import org.deepdive.datastore._
import org.deepdive.extraction.{ExtractionManager, ExtractionTask}
import org.deepdive.inference.{InferenceManager}
import org.deepdive.inference.InferenceNamespace
import org.deepdive.profiling._
import scala.concurrent.duration._
import scala.concurrent.{Future, Await}
import scala.language.postfixOps
import scala.io.Source
import scala.util.{Try, Success, Failure}

object DeepDive extends Logging {

  def run(config: Config, outputDir: String) {

    // Initialize and get the actor system
    val system = Context.system

    // catching all exceptions and terminate Akka
    try {

    // Load Settings
    val settings = SettingsParser.loadFromConfig(config)

    // If relearn_from specified, set output dir to that dir and skip everything
    val relearnFrom = settings.pipelineSettings.relearnFrom

    log.debug(s"relearnFrom=${relearnFrom}")

    Context.outputDir = relearnFrom match {
      case null => outputDir
      case _ => relearnFrom
    }

    // Create the output directory
    val outputDirFile = new File(outputDir)
    outputDirFile.mkdirs()
    log.debug(s"outputDir=${Context.outputDir}")

    // Setup the data store
    JdbcDataStoreObject.init(config)
    settings.schemaSettings.setupFile.foreach { file =>
      log.info(s"Setting up the schema using ${file}")
      val cmd = Source.fromFile(file).getLines.mkString("\n")
      JdbcDataStoreObject.executeSqlQueries(cmd)
    }

    implicit val timeout = Timeout(1337 hours)
    implicit val ec = system.dispatcher

    // Start actors
    val profiler = system.actorOf(Profiler.props, "profiler")
    val taskManager = system.actorOf(TaskManager.props, "taskManager")
    val inferenceManager = system.actorOf(InferenceManager.props(
      taskManager, settings.schemaSettings.variables, settings.dbSettings), "inferenceManager")
    val extractionManager = system.actorOf(
      ExtractionManager.props(settings.extractionSettings.parallelism, settings.dbSettings),
      "extractionManager")

    // Build tasks for extractors
    val extractionTasks = for {
      extractor <- settings.extractionSettings.extractors
      extractionTask = ExtractionTask(extractor)
    } yield Task(s"${extractor.name}", extractor.dependencies.toList,
      extractionTask, extractionManager)

    // Build task to construct the factor graph
    val activeFactors = settings.pipelineSettings.activePipeline match {
      case Some(pipeline) =>
        settings.inferenceSettings.factors.filter(f => pipeline.tasks.contains(f.name))
      case None => settings.inferenceSettings.factors
    }

    val groundFactorGraphMsg = InferenceManager.GroundFactorGraph(
      activeFactors, settings.calibrationSettings,
        settings.inferenceSettings.skipLearning, settings.inferenceSettings.weightTable
    )
    val groundFactorGraphTask = Task("inference_grounding", extractionTasks.map(_.id),
      groundFactorGraphMsg, inferenceManager)

    val inferenceTask = Task("inference", extractionTasks.map(_.id) ++ Seq("inference_grounding"),
      InferenceManager.RunInference(activeFactors, settings.calibrationSettings.holdoutFraction,
        settings.calibrationSettings.holdoutQuery, settings.samplerSettings.samplerCmd,
        settings.samplerSettings.samplerArgs, settings.pipelineSettings, settings.dbSettings),
        inferenceManager, true)

    val reportingTask = Task("report", List("calibration") ++ extractionTasks.map(_.id), Profiler.PrintReports, profiler, false)

    val terminationTask = Task("shutdown", List("report"), TaskManager.Shutdown, taskManager, false)

    log.debug(s"Total number of extractors: ${settings.extractionSettings.extractors.size}")
    log.debug(s"Total number of factors: ${settings.inferenceSettings.factors.size}")

    // If no extractors and factors, no tasks should be run
    val allTasks = settings.extractionSettings.extractors.size + settings.inferenceSettings.factors.size match {
      case 0 =>
        List(reportingTask, terminationTask)
      case _ =>
        extractionTasks ++ Seq(groundFactorGraphTask) ++
        List(inferenceTask, reportingTask, terminationTask)
    }

    // Create a default pipeline that executes all tasks
    val defaultPipeline = Pipeline("_default",
      activeFactors.size match {
        // If no factors are active, do not run inference tasks
        case 0 => allTasks.map(_.id).toSet -- Set("inference_grounding", "inference", "calibration")
        case _ => allTasks.map(_.id).toSet
      })

    // Create a pipeline that runs only from learning
    val relearnPipeline = Pipeline("_relearn", Set("inference", "calibration", "report", "shutdown"))


    log.debug(s"Number of active factors: ${activeFactors.size}")
    // If no factors are active, do not do grounding, inference and calibration
    val postExtraction = activeFactors.size match {
      case 0 =>
        Set("report", "shutdown")
      case _ => Set("inference_grounding", "inference", "calibration", "report", "shutdown")
    }
    if (activeFactors.size == 0) {
      log.info("No active factors. Skip inference.")
    }
    // Figure out which pipeline to run
    val activePipeline = relearnFrom match {
      case null =>
          settings.pipelineSettings.activePipeline match {
            case Some(pipeline) => pipeline.copy(tasks = pipeline.tasks ++
              postExtraction)
            case None => defaultPipeline
          }
      case _ => relearnPipeline
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

    // end try
    } catch {
      /* Notes @zifei:
        This non-termination fix does not guarantee fixing all
        non-termination errors, since we has multiple Akka actors
        (InferenceManager, ExtractionManager, etc), and simply catching
        errors in DeepDive class may not handle all cases.

        But this try-catch do fix some errors, e.g. invalid configuration
        file (mandatory fields are not present) (in: Settings.loadFromConfig).
        Tested in BrokenTest.scala
      */
      case e: Exception =>
        // In case of any exception
        log.error(e.getMessage)
        Context.shutdown()
        throw e
    }
  }

}
