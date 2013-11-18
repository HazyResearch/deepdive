package org.deepdive

import akka.actor.ActorSystem
import akka.event.Logging
import com.typesafe.config._
import org.deepdive.context.ContextManager
import org.deepdive.inference.InferenceManager
import org.deepdive.context.Settings
import org.deepdive.extraction.{ExtractionManager, ExtractionTask}

object Pipeline {

  def run(config: Config) {

    // Parse Settings
    val settings = Settings.loadFromConfig(config)

    // Start an actor system
    val system = ActorSystem("deepdive")
    val log = Logging.getLogger(system, this)

    // Start the Context manager
    val contextManager = system.actorOf(ContextManager.props, "ContextManager")

    // Start the Inference Manager
    val inferenceManager = system.actorOf(InferenceManager.props(contextManager, Settings.databaseUrl), "InferenceManager")

    // TODO: ETL Tasks: We ignore this for now

    // TODO: Have an extraction manager that manages and parallelizes the extractions
    // Start the ExtractorExecutor for each defined Extractor
    val extractionManager = system.actorOf(ExtractionManager.props(Settings.databaseUrl), "ExtractionManager")
    // val extractorExecutor = system.actorOf(ExtractorExecutor.props(Settings.databaseUrl), "ExtractorExecutor")
    Settings.get().extractors.foreach { extractor =>
      extractionManager ! ExtractionManager.AddTask(
        ExtractionTask(extractor.name, extractor.outputRelation, extractor.inputQuery, extractor.udf))
    }
  
    // TODO: Generate factor graph

    system.awaitTermination()
    // Thread.sleep(5000)
  }

}