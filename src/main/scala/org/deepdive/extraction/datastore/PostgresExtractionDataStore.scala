package org.deepdive.extraction.datastore

import anorm._
import au.com.bytecode.opencsv.CSVWriter
import java.io.{File, StringWriter, FileWriter}
import java.lang.RuntimeException
import java.sql.Connection
import java.util.concurrent.atomic.AtomicLong
import org.deepdive.Context
import org.deepdive.datastore.{PostgresDataStore, DataStoreUtils}
import org.deepdive.Logging
import org.deepdive.settings._
import scala.collection.JavaConversions._
import spray.json._
import spray.json.DefaultJsonProtocol._

trait PostgresExtractionDataStoreComponent extends ExtractionDataStoreComponent {

  val dataStore = new PostgresExtractionDataStore

  class PostgresExtractionDataStore extends ExtractionDataStore[JsObject] with Logging {

    /* Globally unique variable id for this data store */
    private val variableIdCounter = new AtomicLong(0)

    def init() = {
      variableIdCounter.set(0)
    }

    def BatchSize = 50000

    def queryAsMap[A](query: String)(block: Iterator[Map[String, Any]] => A) : A = {
      PostgresDataStore.withConnection { implicit conn =>
        val iter = SQL(query)().map { row =>
          row.asMap.toMap.mapValues { 
            case x : org.postgresql.jdbc4.Jdbc4Array => x.getArray()
            case x : java.sql.Date => x.toString
            case other => other
          }
        }.iterator
        block(iter)
      }
    }

    def queryAsJson[A](query: String)(block: Iterator[JsObject] => A) : A = {
      queryAsMap(query) { iter =>
        val jsonIter = iter.map { row =>
          JsObject(row.mapValues(anyValToJson))
        }
        block(jsonIter)
      }
    }

    def write(result: Seq[JsObject], outputRelation: String) : Unit = {
      PostgresDataStore.withConnection { implicit connection =>
        if (result.size == 0) {
          log.info("nothing to write.")
          return
        }

        // We sample the keys to figure out which fields to insert into
        // This is a ugly, but using this we don't need an explicit schema definition.
        // Is there a better way?
        val sampledKeys = result.take(100).flatMap(_.fields.keySet).toSet
        val copySQL = buildCopySql(outputRelation, sampledKeys)

        // Build the dataset as a TSV string
        val tmpFile = buildCopyData(result, sampledKeys)

        log.info(s"Writing extraction result to postgres. length=${result.length}, sql=${copySQL}")
        PostgresDataStore.copyBatchData(copySQL, tmpFile)
        log.info(s"Wrote num=${result.length} records.")
      }
    }

    /* Builds a COPY statement for a given relation and column names */
    def buildCopySql(relationName: String, keys: Set[String]) = {
      val fields =  List("id") ++ keys.filterNot(_ == "id").toList.sorted
      s"""COPY ${relationName}(${fields.mkString(", ")}) FROM STDIN CSV"""
    }

    /* Builds a CSV dat astring for given JSON data and column names */
    def buildCopyData(data: Seq[JsObject], keys: Set[String]) : File = {
      val tmpFile = File.createTempFile("csv", "")
      val writer = new CSVWriter(new FileWriter(tmpFile))
      for (obj <- data) { 
        val dataList = obj.fields.filterKeys(_ != "id").toList.sortBy(_._1)
        val strList = dataList.map (x => jsValueToString(x._2))
        // We get a unique id for the record
        val id = variableIdCounter.getAndIncrement()
        writer.writeNext((List(id.toString) ++ strList).toArray)
      }
      writer.close()
      tmpFile
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
      case x : JsObject => x.compactPrint
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
      case x : org.postgresql.util.PGobject =>
        x.getType match {
          case "json" => x.getValue.asJson
          case _ =>
            log.error(s"Could not convert ${x.toString} of type=${x.getType} to JSON")
            JsNull
        }
      case x =>
        log.error(s"Could not convert ${x.toString} of type=${x.getClass.getName} to JSON")
        JsNull
    }

  }

}
