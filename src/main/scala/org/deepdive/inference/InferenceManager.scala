package org.deepdive.inference

import akka.actor.{Actor, ActorRef, ActorLogging, Props}

/* Manages the Factor and Variable relations in the database */
class InferenceManager(contextManager: ActorRef, databaseUrl: String) extends Actor with ActorLogging {

  val factorGraphBuilder = context.actorOf(FactorGraphBuilder.props)

  override def preStart() {
    log.debug("Starting")
  }

  def receive = {
    case msg : FactorGraphBuilder.AddFactorsForRelation =>
      log.debug(s"Adding factors for ${msg.relation.name}")
      factorGraphBuilder forward msg
    case other =>
      log.debug("Huh?")
  }
}

object InferenceManager {
  def props(contextManager: ActorRef, databaseUrl: String) : Props = 
    Props(classOf[InferenceManager], contextManager, databaseUrl)
}