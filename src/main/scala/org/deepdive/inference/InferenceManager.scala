package org.deepdive.inference

import akka.actor.{Actor, ActorRef, ActorLogging, Props}

/* Manages the Factor and Variable relations in the database */
trait InferenceManager extends Actor with ActorLogging {
  self: InferenceDataStoreComponent =>

  val factorGraphBuilder = context.actorOf(FactorGraphBuilder.props)

  override def preStart() {
    log.info("Starting")
  }

  def receive = {
    case msg : FactorGraphBuilder.AddFactorsAndVariables =>
      factorGraphBuilder forward msg
    case InferenceManager.WriteInferenceResult(file) =>
      log.info("writing inference result back to datastore")
      inferenceDataStore.writeInferenceResult(file)
      sender ! "Done"
    case other =>
      log.warning("Huh?")
  }
}

object InferenceManager {

  // TODO: Refactor this
  class PostgresInferenceManager extends InferenceManager with 
    PostgresInferenceDataStoreComponent

  def props : Props = Props(classOf[PostgresInferenceManager])

  // Messages
  case class WriteInferenceResult(file: String)

}