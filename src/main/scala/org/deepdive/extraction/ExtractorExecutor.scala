package org.deepdive.extraction

import spray.json._
import akka.actor.{Actor, ActorRef, Props, ActorLogging}
import org.deepdive.datastore.{PostgresDataStore => DB}

case class ExtractionTask(outputRelation: String, inputQuery: String, udf: String)

object ExtracorExecutor {
  def props(databaseUrl: String): Props = Props(classOf[ExtracorExecutor], databaseUrl)
}

class ExtracorExecutor(databaseUrl: String) extends Actor with ActorLogging {

  override def preStart() {
    log.debug("Starting")
  }

  case class Execute(task: ExtractionTask)

  def receive = {
    case Execute(task) => 
      val executor = new ScriptTaskExecutor(task, databaseUrl)
      val result = executor.run()
      writeResult(result)
  }

  private def doExecute(task: ExtractionTask) {
    log.debug("Executing $task")
  }

  private def writeResult(result: List[JsValue]) {
    DB.withConnection { implicit conn =>
      // TODO insert data here
    }
  }

}