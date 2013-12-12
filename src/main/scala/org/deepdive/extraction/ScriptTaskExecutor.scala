package org.deepdive.extraction

import akka.event.Logging
import anorm._ 
import java.io.{File, PrintWriter}
import java.sql.Connection
import org.deepdive.Logging
import org.deepdive.datastore.{PostgresDataStore => DB}
import scala.collection.mutable.ArrayBuffer
import scala.io.Source
import scala.sys.process._
import spray.json._
import scala.collection.JavaConversions._
import spray.json.DefaultJsonProtocol._

object ScriptTaskExecutor extends Logging {

  def valToJson(x: Any) : JsValue = x match {
    case Some(x) => valToJson(x)
    case None | null => JsNull
    case x : String => x.toJson
    case x : Boolean => x.toJson
    case x : Int => x.toJson
    case x : Long => x.toJson
    case x : Double => x.toJson
    case x : org.postgresql.jdbc4.Jdbc4Array => x.getArray().asInstanceOf[Array[_]].map(valToJson).toJson
    case x =>
      log.error("Could not convert type ${x.getClass.name} to JSON")
      JsNull
  }

  def sqlRowToJson(row: SqlRow) : Map[String, JsValue] = {
    row.asMap.toMap.mapValues(valToJson)
  }
}

class ScriptTaskExecutor(task: ExtractionTask) extends Logging {

  def run() : ExtractionResult = {
    
    // Set the script to be executable
    val file = new File(task.udf)
    file.setExecutable(true)

    log.info(s"Running UDF: ${file.getAbsolutePath}")

    // Result will be stored here
    val result : ArrayBuffer[JsValue] = ArrayBuffer[JsValue]();

    DB.withConnection { implicit conn =>

      // Query for the input data
      val inputData = SQL(task.inputQuery)().map { row =>
        JsObject(ScriptTaskExecutor.sqlRowToJson(row))
      }

      log.debug(s"Streaming num=${inputData.size} tuples.")

      val io = new ProcessIO(
        in => { 
          val writer = new PrintWriter(in, true)
          inputData.foreach { tuple => writer.println(tuple) }
          in.close()
        },
        out => {
          Source.fromInputStream(out).getLines.map(_.asJson).foreach { tuple => result += tuple }
          out.close()
        },
        err => {
          Source.fromInputStream(err).getLines.foreach(println)
        }
      ).daemonized()
      val process = task.udf run(io)
      process.exitValue()
    }

    log.debug(s"UDF process has exited. Generated num=${result.size} records.")
    ExtractionResult(result.map(_.asInstanceOf[JsObject]).toList)

  }

}