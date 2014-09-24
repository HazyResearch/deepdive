package org.deepdive.extraction.datastore

import au.com.bytecode.opencsv.CSVWriter
import java.io.{ File, StringWriter, FileWriter, PrintWriter, BufferedWriter, Writer }
import java.lang.RuntimeException
import java.sql.Connection
import java.util.concurrent.atomic.AtomicLong
import org.deepdive.Context
import org.deepdive.datastore.{ MysqlDataStore, DataStoreUtils }
import org.deepdive.Logging
import org.deepdive.settings._
import scala.collection.JavaConversions._
import play.api.libs.json._
import scala.util.{ Try, Success, Failure }
import scalikejdbc._

trait MysqlExtractionDataStoreComponent extends ExtractionDataStoreComponent {
  val dataStore = new MysqlExtractionDataStore
}

class MysqlExtractionDataStore extends ExtractionDataStore[JsObject] with JdbcExtractionDataStore with Logging {

  def init() = {
  }

  def ds = MysqlDataStore

  /* 
   * Writes a list of tuples back to the datastore.
   * IMPORTANT: This method must assign a globally unique variable id to each record 
   */
  def addBatch(result: Iterator[JsObject], outputRelation: String): Unit = {
    throw new RuntimeException(s"method addBatch in ${this.getClass} is not implemented")
  }

  /* Builds a COPY statement for a given relation and column names */
  def buildCopySql(relationName: String, keys: Set[String]) = {
    throw new RuntimeException(s"method buildCopySql in ${this.getClass} is not implemented")
  }

  /* Builds a CSV dat astring for given JSON data and column names */
  def writeCopyData(data: Iterator[JsObject], fileWriter: Writer): Unit = {
    throw new RuntimeException(s"method writeCopyData in ${this.getClass} is not implemented")
  }
  /* Translates a JSON value to a String that can be insert using COPY statement */
  private def jsValueToString(x: JsValue): String = x match {
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
    case x: JsObject => Json.stringify(x)
    case _ =>
      log.warning(s"Could not convert JSON value ${x} to String")
      ""
  }

}
