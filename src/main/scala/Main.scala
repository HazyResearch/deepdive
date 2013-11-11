package org.deepdive

import akka.actor.ActorSystem
import org.deepdive.context.ContextManager
import org.deepdive.inference.InferenceManager
import org.deepdive.context.Settings
import org.deepdive.extraction.{ExtracorExecutor, ExtractionTask}

/* DeepDive main entry point */
object Main extends Application {

  // Parse Settings
  val settings = Settings.loadDefault()

  // Start an actor system
  val system = ActorSystem("deepdive")

  // Start the Context manager
  val contextManager = system.actorOf(ContextManager.props)

  // Start the Inference Manager
  val inferenceManager = system.actorOf(InferenceManager.props(contextManager, Settings.databaseUrl))

  // TODO: ETL Tasks: We ignore this for now

  // TODO: Have an extraction manager that manages and parallelizes the extractions
  // Start the ExtractorExecutor for each defined Extractor
  val extractorExecutor = system.actorOf(ExtracorExecutor.props(Settings.databaseUrl))
  Settings.get().extractors.foreach { extractor =>
    extractorExecutor ! ExtractionTask(extractor.outputRelation, extractor.inputQuery, extractor.udf)
  }
  

  // Run until the system terminates
  system.awaitTermination()



}