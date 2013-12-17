package org.deepdive.extraction.datastore

import anorm._
import java.lang.RuntimeException
import org.deepdive.context._
import org.deepdive.settings._
import org.deepdive.datastore.PostgresDataStore
import org.deepdive.datastore.Utils._
import org.deepdive.Logging
import spray.json._
import spray.json.DefaultJsonProtocol._
import java.io.{ByteArrayInputStream, StringWriter}
import au.com.bytecode.opencsv.CSVWriter

trait PostgresExtractionDataStoreComponent extends ExtractionDataStoreComponent {

  val dataStore = new PostgresExtractionDataStore

  class PostgresExtractionDataStore extends ExtractionDataStore with Logging {

    implicit lazy val connection = PostgresDataStore.borrowConnection()

    /* How many tuples to insert at once */
    val BATCH_SIZE = 5000

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


    def writeResult(result: List[JsObject], outputRelation: String) : Unit = {
      // We sample the keys to figure out which fields to insert into
      // This is a ugly, but using this we don't need an explicit schema definition.
      // Is there a better way?
      val sampledKeys = result.take(BATCH_SIZE).flatMap(_.fields.keySet).toSet


      // We use Postgres' copy manager isntead of anorm to do efficient batch inserting
      // Do some magic to ge the underlying connection
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

    def queryAsMap(query: String) : Stream[Map[String, Any]] = {
      SQL(query)().map { row =>
        row.asMap.toMap
      }
    }

    def queryAsJson(query: String) : Stream[JsObject] = {
      queryAsMap(query).map { row =>
        JsObject(row.mapValues(valToJson))
      }
    }

    private def jsValueToString(x: JsValue) : String = x match {
      case JsString(x) => x
      case JsNumber(x) => x.toString
      case JsNull => ""
      case JsBoolean(x) => x.toString
      case _ => 
        log.warning(s"Could not convert JSON value ${x} to String")
        ""
    }

    private def valToJson(x: Any) : JsValue = x match {
      case Some(x) => valToJson(x)
      case None | null => JsNull
      case x : String => x.toJson
      case x : Boolean => x.toJson
      case x : Int => x.toJson
      case x : Long => x.toJson
      case x : Double => x.toJson
      case x : org.postgresql.jdbc4.Jdbc4Array => x.getArray().asInstanceOf[Array[_]].map(valToJson).toJson
      case x =>
        log.error("Could not convert ${x.toString} of type=${x.getClass.name} to JSON")
        JsNull
    }

  }

}