package org.deepdive.extraction.datastore

import anorm._
import java.lang.RuntimeException
import org.deepdive.Context
import org.deepdive.settings._
import org.deepdive.datastore.{PostgresDataStore, DataStoreUtils}
import org.deepdive.Logging
import spray.json._
import spray.json.DefaultJsonProtocol._
import java.io.{FileReader, ByteArrayInputStream, StringWriter, FileWriter}
import au.com.bytecode.opencsv.CSVWriter
import scala.collection.JavaConversions._

trait PostgresExtractionDataStoreComponent extends ExtractionDataStoreComponent {

  val dataStore = new PostgresExtractionDataStore

  class PostgresExtractionDataStore extends ExtractionDataStore with Logging {

    implicit lazy val connection = PostgresDataStore.borrowConnection()

    def BatchSize = 100000

    def queryAsMap(query: String) : Stream[Map[String, Any]] = {
      SQL(query)().map { row =>
        row.asMap.toMap.mapValues { 
          case x : org.postgresql.jdbc4.Jdbc4Array => x.getArray()
          case x : java.sql.Date => x.toString
          case other => other
        }
      }
    }

    def queryAsJson(query: String) : Stream[JsObject] = { 
      queryAsMap(query).map { row =>
        JsObject(row.mapValues(anyValToJson))
      }
    }

    def write(result: List[JsObject], outputRelation: String) : Unit = {

      if (result.size == 0) {
        log.info("nothing to write.")
        return
      }

      // We sample the keys to figure out which fields to insert into
      // This is a ugly, but using this we don't need an explicit schema definition.
      // Is there a better way?
      val sampledKeys = result.take(100).flatMap(_.fields.keySet).toSet

      // We use Postgres' copy manager isntead of anorm to do efficient batch inserting
      // Need to do do some magic to get the underlying connection
      val del = new org.apache.commons.dbcp.DelegatingConnection(connection)
      val pg_conn = del.getInnermostDelegate().asInstanceOf[org.postgresql.core.BaseConnection]
      val cm = new org.postgresql.copy.CopyManager(pg_conn)
      val copySQL = buildCopySql(outputRelation, sampledKeys)

      log.info(s"Writing extraction result to postgres. length=${result.length}, sql=${copySQL}")

      // Build the dataset as a TSV string
      val strData = buildCopyData(result, sampledKeys)
      val is = new ByteArrayInputStream(strData.getBytes)
      cm.copyIn(copySQL, is)

      log.info(s"Wrote num=${result.length} records.")
    }

    /* Builds a COPY statement for a given relation and column names */
    def buildCopySql(relationName: String, keys: Set[String]) = {
      val fields = keys.filterNot(_ == "id").toList.sorted
      s"""COPY ${relationName}(${fields.mkString(", ")}) FROM STDIN CSV"""
    }

    /* Builds a CSV dat astring for given JSON data and column names */
    def buildCopyData(data: List[JsObject], keys: Set[String]) = {
      val strWriter = new StringWriter()
      val writer = new CSVWriter(strWriter)
      data.foreach { obj =>
        val dataList = obj.fields.filterKeys(_ != "id").toList.sortBy(_._1)
        val strList = dataList.map (x => jsValueToString(x._2))
        writer.writeNext(strList.toArray)
      }
      writer.close()
      strWriter.toString
    }

    /* Translates a JSON value to a String that can be insert using COPY statement */
    private def jsValueToString(x: JsValue) : String = x match {
      case JsString(x) => x
      case JsNumber(x) => x.toString
      case JsNull => null
      case JsBoolean(x) => x.toString
      case JsArray(x) => "{" + x.map {
        // This is ugly, but we need to quote strings
        case JsString(x) => jsValueToString(JsString(s""" "${x}" """))
        case x: JsValue => jsValueToString(x)
      }.map (ele => s"""${ele}""").mkString(",") + "}"
      case _ =>
        log.warning(s"Could not convert JSON value ${x} to String")
        ""
    }

    /* Translates an arbitary values that comes back from the database to a JSON value */
    private def anyValToJson(x: Any) : JsValue = x match {
      case Some(x) => anyValToJson(x)
      case None | null => JsNull
      case x : String => JsString(x)
      case x : Boolean => JsBoolean(x)
      case x : Int => JsNumber(x)
      case x : Long => JsNumber(x)
      case x : Double => JsNumber(x)
      case x : java.sql.Date => JsString(x.toString)
      case x : Array[_] => JsArray(x.toList.map(x => anyValToJson(x)))
      case x : org.postgresql.jdbc4.Jdbc4Array => 
        JsArray(x.getArray().asInstanceOf[Array[_]].map(anyValToJson).toList)
      case x =>
        log.error(s"Could not convert ${x.toString} of type=${x.getClass.getName} to JSON")
        JsNull
    }

  }

}
