package org.deepdive.extraction

import anorm._
import spray.json._
import akka.actor.{Actor, ActorRef, Props, ActorLogging}
import org.deepdive.context.{Relation, Settings}
import org.deepdive.datastore.{PostgresDataStore => DB}

case class ExtractionTask(outputRelation: String, inputQuery: String, udf: String)

object ExtractorExecutor {
  def props(databaseUrl: String): Props = Props(classOf[ExtractorExecutor], databaseUrl)
  case class Execute(task: ExtractionTask)

  def buildInsert(rows: List[JsArray], relation: Relation) = {
    val relation_fields =  "(" + relation.schema.keys.filterNot(_ == "id").mkString(", ") + ")"
    val values = rows.map { row =>
      "(" + row.elements.map(_.compactPrint).mkString(", ").replaceAll("\"","'") + ")"
    }.mkString(", ")
    s"INSERT INTO ${relation.name} ${relation_fields} VALUES ${values};"
  }

}

class ExtractorExecutor(databaseUrl: String) extends Actor with ActorLogging {

  override def preStart() {
    log.debug("Starting")
  }

  
  def receive = {
    case ExtractorExecutor.Execute(task) => 
      doExecute(task)
    case _ =>
      log.warning("Huh?")
  }

  private def doExecute(task: ExtractionTask) {
    log.debug(s"Executing $task")
    val executor = new ScriptTaskExecutor(task, databaseUrl)
    val result = executor.run()
    writeResult(result, task.outputRelation)
  }

  private def writeResult(result: List[JsArray], outputRelation: String) {
    log.debug(s"Writing extraction result back to the database, length=${result.length}")
    val insertStatement = ExtractorExecutor.buildInsert(result, Settings.getRelation(outputRelation).orNull)
    DB.withConnection { implicit conn =>
      SQL(insertStatement).execute()
    }
  }

}