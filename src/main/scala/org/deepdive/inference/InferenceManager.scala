package org.deepdive.inference

import akka.actor.{Actor, ActorRef, ActorLogging, Props}

/* Manages the Factor and Variable relations in the database */
class InferenceManager(contextManager: ActorRef, databaseUrl: String) extends Actor with ActorLogging {

  case class AddFactor(id: Integer)
  case class AddVariable(id: Integer)

  override def preStart() {
    log.debug("Starting")
  }

  def receive = {
    case AddFactor(id) =>
      log.debug("Adding factor=$id")
    case AddVariable(id) =>
      log.debug("Adding variable=$id")
  }
}

object InferenceManager {
  def props(contextManager: ActorRef, databaseUrl: String) : Props = 
    Props(classOf[InferenceManager], contextManager, databaseUrl)
}