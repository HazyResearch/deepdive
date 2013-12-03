package org.deepdive

import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config._
import java.io.File
import org.deepdive.context.{Context, Settings}
import org.deepdive.context.{ContextManager, RelationTaskOrdering}
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
    Settings.loadFromConfig(config)    

    // Initialize the data store
    PostgresDataStore.init(Settings.databaseUrl, Settings.get().connection.user, Settings.get().connection.password)

    // Start the Context manager
    val contextManager = system.actorOf(ContextManager.props, "ContextManager")

    // Start the Inference Manager
    val inferenceManager = system.actorOf(InferenceManager.props(contextManager, Settings.databaseUrl), "InferenceManager")

    // TODO: ETL Tasks: We ignore this for now

    // TODO: Have an extraction manager that manages and parallelizes the extractions
    // Start the ExtractorExecutor for each defined Extractor
    val extractionManager = system.actorOf(ExtractionManager.props, "ExtractionManager")
    // val extractorExecutor = system.actorOf(ExtractorExecutor.props(Settings.databaseUrl), "ExtractorExecutor")

    implicit val timeout = Timeout(30 minutes)
    implicit val ec = system.dispatcher
    
    // Run extractions
    log.debug("Running extractors")
    val extractionResults = for {
      extractor <- Settings.get().extractors
      relation <- Settings.getRelation(extractor.outputRelation)
      task <- Some(ExtractionTask(extractor.name, extractor.outputRelation, extractor.inputQuery, extractor.udf))
      extractionResult <- Some(ask(extractionManager, ExtractionManager.AddTask(task)))
    } yield extractionResult

    Await.result(Future.sequence(extractionResults), 30 minutes)

    // Build the factor graph
    log.debug("Building factor graph")
    val graphResults = for {
      relation <- Settings.get().relations.sorted(RelationTaskOrdering)
      factor <- Settings.extractorForRelation(relation.name).map(_.factor)
      graphResult <- Some(ask(inferenceManager, 
        FactorGraphBuilder.AddFactorsForRelation(relation.name, relation, factor)))
    } yield graphResult

    Await.result(Future.sequence(graphResults), 30 minutes)

    // Write result
    PostgresDataStore.withConnection { implicit conn =>
      FileGraphWriter.dump(new File("target/variables.tsv"), new File("target/factors.tsv"), new File("target/weights.tsv"))
    }

    system.shutdown()
    system.awaitTermination()

  }

}