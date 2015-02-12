package org.deepdive.datastore

import java.io.{File, Reader, Writer, FileReader, FileWriter, BufferedReader, BufferedWriter, PrintWriter, InputStream, InputStreamReader}
import java.sql.Connection
import org.deepdive.Logging
import play.api.libs.json._


trait MysqlDataStoreComponent extends JdbcDataStoreComponent {
  def dataStore = new MysqlDataStore
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

  /**
   * Drop and create a sequence, based on database type.
   *
   * @see http://dev.mysql.com/doc/refman/5.0/en/user-variables.html
   * @see http://www.it-iss.com/mysql/mysql-renumber-field-values/
   */
  override def createSequenceFunction(seqName: String): String = s"SET @${seqName} = -1;"

  /**
   * Get the next value of a sequence
   */
  override def nextVal(seqName: String): String = s" @${seqName} := @${seqName} + 1 "

  /**
   * Cast an expression to a type
   */
  override def cast(expr: Any, toType: String): String = 
    toType match {
      // convert text/varchar to char(N) where N is max length of given
      case "text" | "varchar" => s"convert(${expr.toString()}, char)"
      // in mysql, convert to unsigned guarantees bigint.
      // @see http://stackoverflow.com/questions/4660383/how-do-i-cast-a-type-to-a-bigint-in-mysql
      case "bigint" | "int" => s"convert(${expr.toString()}, unsigned)"
      case "real" | "float" | "double" => s"${expr.toString()} + 0.0"
      // for others, try to convert as it is expressed.
      case _ => s"convert(${expr.toString()}, ${toType})"
    }
  
  /**
   * Concatinate multiple strings use "concat" function in mysql
   */
  override def concat(list: Seq[String], delimiter: String): String = {
    list.length match {
      // return a SQL empty string if list is empty
      case 0 => "''" 
      case _ =>
      delimiter match {
        case null => s"concat(${list.mkString(", ")})"
        case "" => s"concat(${list.mkString(", ")})"
        case _ => s"concat(${list.mkString(s",'${delimiter}',")})"
      }
    }
  }

  /**
   * ANALYZE TABLE
   */
  override def analyzeTable(table: String) = s"ANALYZE TABLE ${table}"
  
  /**
   * Given a string column name, Get a quoted version dependent on DB.
   *
   *          if psql, return "column"
   *          if mysql, return `column`
   */
  override def quoteColumn(column: String): String = '`' + column + '`'
  
  override def randomFunction: String = "RAND()"

  // this function is specific for greenplum
  override def createAssignIdFunctionGreenplum() = {
    // nothing
  }

  // assign senquential ids to table's id column
  override def assignIds(table: String, startId: Long, sequence: String) : Long = {
    executeSqlQueries(s"UPDATE ${table} SET id = ${nextVal(sequence)};")
    var count : Long = 0
    executeSqlQueryWithCallback(s"""SELECT COUNT(*) FROM ${table};""") { rs =>
      count = rs.getLong(1)
    }
    return count
  }


}