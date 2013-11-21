package org.deepdive.extraction

import akka.actor.{Actor, ActorRef, Props, ActorLogging, FSM}
import scala.collection.mutable.Map
import scala.concurrent.duration._

object ExtractionManager {
  // Messages
  sealed trait Message
  case class AddTask(task: ExtractionTask) extends Message
  case class TaskCompleted(name: String) extends Message
  
  // States
  sealed trait State
  case object Idle extends State
  case object Active extends State

  // Data
  sealed trait Data
  case class Tasks(queue: Seq[ExtractionTask]) extends Data
  
  // Props
  def props(databaseUrl: String) : Props = Props(classOf[ExtractionManager], databaseUrl)
}

// This actor is an FSM that shuts down after a given timeout
class ExtractionManager(databaseUrl: String) extends Actor 
  with FSM[ExtractionManager.State, ExtractionManager.Tasks] with ActorLogging {
  import ExtractionManager._

  startWith(Idle, Tasks(Nil))
    
  val taskListeners = Map[String, ActorRef]()
  val executor = context.actorOf(ExtractorExecutor.props(databaseUrl), "ExtractorExecutor")


  override def preStart(){
    log.debug("Starting")
  }

  when(ExtractionManager.Idle, stateTimeout = 5.seconds) {
    case Event(AddTask(task), currentTasks) => 
      sendTask(task)
      goto(Active) using Tasks(currentTasks.queue :+ task)
    case Event(StateTimeout, _) => 
      // Nothing to do
      stay
  }

  when(ExtractionManager.Active) {
    case Event(AddTask(task), currentTasks) => 
      sendTask(task)
      stay using Tasks(currentTasks.queue :+ task)
    case Event(TaskCompleted(name), currentTasks) =>
      log.debug(s"Completed task $name. Removing from the queue.")
      val newQueue = currentTasks.queue.filterNot { _.name == name}
      
      // Notify the original task sender of the completion
      taskListeners.get(name).foreach(_ ! TaskCompleted(name))
      
      if(newQueue.size == 0) {
        goto(Idle) using Tasks(newQueue)
      } else {
        stay using Tasks(newQueue)
      }
  }

  onTransition {
    case Active -> Idle => log.debug("Moving from active to idle state")
    case Idle -> Active => log.debug("Moving from idle to active state")
  }

  private def sendTask(task: ExtractionTask) {
    taskListeners += Tuple2(task.name, sender)
    log.debug(s"Sending ${task.name} to executor.")
    executor ! ExtractorExecutor.Execute(task)
  }

  initialize()

}