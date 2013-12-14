package org.deepdive.inference

import akka.actor.{Actor, ActorRef, ActorLogging, Props}

/* Manages the Factor and Variable relations in the database */
class InferenceManager extends Actor with ActorLogging {

  val factorGraphBuilder = context.actorOf(FactorGraphBuilder.props)

  override def preStart() {
    log.info("Starting")
  }

  def receive = {
    case msg : FactorGraphBuilder.AddFactorsAndVariables =>
      factorGraphBuilder forward msg
    case other =>
      log.warning("Huh?")
  }
}

object InferenceManager {
  def props : Props = Props(classOf[InferenceManager])
}