package org.deepdive.extraction.datastore

import anorm._
import au.com.bytecode.opencsv.CSVWriter
import java.io.{File, StringWriter, FileWriter, PrintWriter, BufferedWriter, Writer}
import java.lang.RuntimeException
import java.sql.Connection
import java.util.concurrent.atomic.AtomicLong
import org.deepdive.Context
import org.deepdive.datastore.{PostgresDataStore, DataStoreUtils}
import org.deepdive.Logging
import org.deepdive.settings._
import scala.collection.JavaConversions._
import play.api.libs.json._
import play.api.libs.json._
import scala.util.{Try, Success, Failure}

trait PostgresExtractionDataStoreComponent extends ExtractionDataStoreComponent {

  val dataStore = new PostgresExtractionDataStore

  class PostgresExtractionDataStore extends ExtractionDataStore[JsObject] with Logging {

    /* Globally unique variable id for this data store */
    private val variableIdCounter = new AtomicLong(0)

    def init() = {
      variableIdCounter.set(0)
    }

    def BatchSize = 20000

    def queryAsMap[A](query: String)(block: Iterator[Map[String, Any]] => A) : A = {
      PostgresDataStore.withConnection { implicit conn =>
        val sqlQuery = SQL(query)
        val statement = sqlQuery.filledStatement
        // If we don't set the fetch size then postgres will return all rows at once 
        // and load them into memory. We don't want that ;)
        // TODO: What is a good value for this?
        conn.setAutoCommit(false)
        statement.setFetchSize(1000)
        val iter = Sql.resultSetToStream(statement.executeQuery()).iterator.map { row =>
          row.asMap.toMap.mapValues { 
            case x : org.postgresql.jdbc4.Jdbc4Array => x.getArray()
            case x : java.sql.Date => x.toString
            case other => other
          }
        }
        block(iter)
      }
    }

    def queryAsJson[A](query: String)(block: Iterator[JsObject] => A) : A = {
      queryAsMap(query) { iter =>
        val jsonIter = iter.map { row =>
          JsObject(row.mapValues(anyValToJson).toSeq)
        }
        block(jsonIter)
      }
    }

    def addBatch(result: Iterator[JsObject], outputRelation: String) : Unit = {
      val file = File.createTempFile(s"deepdive_$outputRelation", ".csv")
      log.info(s"Writing data of to file=${file.getCanonicalPath}")
      val writer = new PrintWriter(new BufferedWriter(new FileWriter(file, true)))
      // Write the dataset to the file for the relation
      writeCopyData(result, writer)
      writer.close()
      val columnNames = scalikejdbc.DB.getColumnNames(outputRelation).toSet
      val copySQL = buildCopySql(outputRelation, columnNames)
      log.info(s"Copying batch data to postgres. sql='${copySQL}'" +
        s"file='${file.getCanonicalPath}'")
      PostgresDataStore.withConnection { implicit connection =>
        Try(PostgresDataStore.copyBatchData(copySQL, file)) match {
          case Success(_) => 
            log.info("Successfully copied batch data to postgres.") 
            file.delete()
          case Failure(ex) => 
            log.error(s"Error during copy: ${ex}")
            log.error(s"Problematic CSV file can be found at file=${file.getCanonicalPath}")
            throw ex
        }
      } 
    }

    /* Builds a COPY statement for a given relation and column names */
    def buildCopySql(relationName: String, keys: Set[String]) = {
      val fields =  List("id") ++ keys.filterNot(_ == "id").toList.sorted
      s"""COPY ${relationName}(${fields.mkString(", ")}) FROM STDIN CSV"""
    }

    /* Builds a CSV dat astring for given JSON data and column names */
    def writeCopyData(data: Iterator[JsObject], fileWriter: Writer) : Unit = {
      val writer = new CSVWriter(fileWriter)
      for (obj <- data) { 
        val dataList = obj.value.filterKeys(_ != "id").toList.sortBy(_._1)
        val strList = dataList.map (x => jsValueToString(x._2))
        // We get a unique id for the record
        val id = variableIdCounter.getAndIncrement()
        writer.writeNext((List(id.toString) ++ strList).toArray)
      }
    }

    /* Translates a JSON value to a String that can be insert using COPY statement */
    private def jsValueToString(x: JsValue) : String = x match {
      case JsString(x) => x.replace("\\", "\\\\")
      case JsNumber(x) => x.toString
      case JsNull => null
      case JsBoolean(x) => x.toString
      case JsArray(x) => 
        val innerData = x.map {
          case JsString(x) => 
            val convertedStr = jsValueToString(JsString(x))
            val escapedStr = convertedStr.replace("\"", "\\\"")
            s""" "${escapedStr}" """ 
          case x: JsValue => jsValueToString(x)
        }.mkString(",")
        val arrayStr = s"{${innerData}}"
        arrayStr
      case x : JsObject => Json.stringify(x)
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
          case "json" => Json.parse(x.getValue)
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
