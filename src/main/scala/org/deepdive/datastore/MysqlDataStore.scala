package org.deepdive.datastore

import java.io.{File, Reader, FileReader, BufferedReader, InputStream, InputStreamReader}
import java.sql.Connection
import org.deepdive.Logging
import scalikejdbc._
import com.mysql.jdbc._

/* Helper object for working with Postgres */
object MysqlDataStore extends JdbcDataStore with Logging {

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
	 
//	  val pg_conn = del.getInnermostDelegate().asInstanceOf[com.mysql.jdbc.Connection]
//      val cm = new org.postgresql.copy.CopyManager(pg_conn)
//      cm.copyIn(sqlStatement, dataReader)
      dataReader.close()
    }

}