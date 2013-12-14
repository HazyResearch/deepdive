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
import scala.concurrent.duration._
import scala.concurrent.{Future, Await}
import scala.util.Sorting

object Pipeline extends Logging {

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
      relation <- Context.settings.findRelation(extractor.outputRelation)
      task = ExtractionTask(extractor)
      extractionResult <- Some(ask(extractionManager, ExtractionManager.AddTask(task)))
    } yield extractionResult

    Await.result(Future.sequence(extractionResults), 30 minutes)

    // Build the factor graph
    log.info("Building factor graph")

    val graphResults = for {
      factor <- Context.settings.factors
      graphResult <- Some(ask(inferenceManager, FactorGraphBuilder.AddFactorsAndVariables(factor)))
    } yield graphResult
    
    Await.result(Future.sequence(graphResults), 30 minutes)
    log.info("Successfully built factor graph.")

    // Write result
    PostgresDataStore.withConnection { implicit conn =>
      FileGraphWriter.dump(new File("target/variables.tsv"), new File("target/factors.tsv"), new File("target/weights.tsv"))
    }

    system.shutdown()
    system.awaitTermination()

  }

}