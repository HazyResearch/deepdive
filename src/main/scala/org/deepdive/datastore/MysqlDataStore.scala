package org.deepdive.datastore

import java.io.{File, Reader, FileReader, BufferedReader, InputStream, InputStreamReader}
import java.sql.Connection
import org.deepdive.Logging
import scalikejdbc._
import com.mysql.jdbc._

import au.com.bytecode.opencsv.CSVWriter
import java.io.{ File, StringWriter, FileWriter, PrintWriter, BufferedWriter, Writer }
import java.lang.RuntimeException
import java.sql.Connection
import java.util.concurrent.atomic.AtomicLong
import org.deepdive.Context
import org.deepdive.Logging
import org.deepdive.settings._
import scala.collection.JavaConversions._
import play.api.libs.json._


trait MysqlDataStoreComponent extends JdbcDataStoreComponent {
  def dataStore = new MysqlDataStore
  def ds = new MysqlDataStore
}

/* Helper object for working with Postgres */
class MysqlDataStore extends JdbcDataStore with Logging {

  def copyBatchData(sqlStatement: String, file: File)(implicit connection: Connection) : Unit = {
    copyBatchData(sqlStatement, new BufferedReader(new FileReader(file))) 
  }

  def copyBatchData(sqlStatement: String, rawData: InputStream)
    (implicit connection: Connection) : Unit = {
    copyBatchData(sqlStatement, new BufferedReader(new InputStreamReader(rawData)))
  }

  /**
   * Do not support this function in mysql
   * Only used for json_extractor?
   */
  // Executes a "COPY FROM STDIN" statement using raw data 
  // TODO zifei: not implemented now
  def copyBatchData(sqlStatement: String, dataReader: Reader)
    (implicit connection: Connection) : Unit = {
      val statement = connection.createStatement()
      val resultSet = statement.executeQuery(sqlStatement)
      dataReader.close()
    }

  /* 
   * Writes a list of tuples back to the datastore.
   * IMPORTANT: This method must assign a globally unique variable id to each record 
   */
  override def addBatch(result: Iterator[JsObject], outputRelation: String): Unit = {
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