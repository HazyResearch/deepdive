package org.deepdive.inference

import akka.actor.{Actor, ActorRef, Props, ActorLogging}
import anorm._
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.ConcurrentHashMap
import org.deepdive.Context
import org.deepdive.extraction.datastore._
import org.deepdive.settings._
import scala.collection.JavaConversions._
import scala.concurrent.duration._
import scala.concurrent.{Future, Await}
import scala.util.{Random, Try, Success, Failure}

object FactorGraphBuilder {

  // Implementation of FactorGraphBuilder using postgres components
  // TODO: Refactor this
  class PostgresFactorGraphBuilder(val variableSchema: Map[String, String]) 
    extends FactorGraphBuilder with PostgresExtractionDataStoreComponent 
    with PostgresInferenceDataStoreComponent

  // Messages
  sealed trait Message
  case class AddFactorsAndVariables(factorDesc: FactorDesc, holdoutFraction: Double, 
    batchSize: Option[Int]) extends Message
}

trait FactorGraphBuilder extends Actor with ActorLogging { 
  self: ExtractionDataStoreComponent with InferenceDataStoreComponent =>
  
  def variableSchema: Map[String, String]

  import FactorGraphBuilder._

  override def preStart() {
    log.info("Starting")
  }

  def receive = {
    case AddFactorsAndVariables(factorDesc, holdoutFraction, batchSize) =>
      log.info(s"Processing factor_name=${factorDesc.name} with holdout_faction=${holdoutFraction}")
      Try(addFactorsAndVariables(factorDesc, holdoutFraction, batchSize)) match {
        case Success(x) => sender ! x
        case Failure(exception) => 
          sender ! akka.actor.Status.Failure(exception)
      }
    case _ => 
  }

  def addFactorsAndVariables(factorDesc: FactorDesc, holdoutFraction: Double, 
    batchSize: Option[Int]) {
    // Query the data store..
    // inferenceDataStore.asInstanceOf[SQLInferenceDataStore]
    //   .groundFactorGraph(factorDesc, holdoutFraction)

  }


}