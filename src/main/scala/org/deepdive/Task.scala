package org.deepdive

import akka.actor.ActorRef

/* 
 * A task (e.g. extractor) which may depends on other tasks 
 * The task description is a message that will be forwarded to the worker
 */
case class Task(id: String, dependencies: List[String], taskDescription: Any, worker: ActorRef, cancellable : Boolean = true)
