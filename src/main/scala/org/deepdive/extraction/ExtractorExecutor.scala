package org.deepdive.extraction

import spray.json._
import akka.actor.{Actor, ActorRef, Props, ActorLogging}
import org.deepdive.datastore.{PostgresDataStore => DB}

case class ExtractionTask(outputRelation: String, inputQuery: String, udf: String)

object ExtracorExecutor {
  def props(databaseUrl: String): Props = Props(classOf[ExtracorExecutor], databaseUrl)
  case class Execute(task: ExtractionTask)
}

class ExtracorExecutor(databaseUrl: String) extends Actor with ActorLogging {

  override def preStart() {
    log.debug("Starting")
  }

  
  def receive = {
    case ExtracorExecutor.Execute(task) => 
      doExecute(task)
    case _ =>
      log.warning("Huh?")
  }

  private def doExecute(task: ExtractionTask) {
    log.debug(s"Executing $task")
    val executor = new ScriptTaskExecutor(task, databaseUrl)
    val result = executor.run()
    writeResult(result)
  }

  private def writeResult(result: List[JsValue]) {
    log.debug(s"Writing extraction result back to the database, length=${result.length}")
    DB.withConnection { implicit conn =>
      // TODO insert data
    }
  }

}