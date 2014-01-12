package org.deepdive.datastore

import java.io.{File, Reader, FileReader, BufferedReader, InputStream, InputStreamReader}
import java.sql.Connection
import org.deepdive.Logging
import scalikejdbc._

/* Helper object for working with Postgres */
object PostgresDataStore extends JdbcDataStore with Logging {

  def copyBatchData(sqlStatement: String, file: File)(implicit connection: Connection) : Unit = {
    copyBatchData(sqlStatement, new BufferedReader(new FileReader(file))) 
  }

  def copyBatchData(sqlStatement: String, rawData: InputStream)
    (implicit connection: Connection) : Unit = {
    copyBatchData(sqlStatement, new BufferedReader(new InputStreamReader(rawData)))
  }

  // Executes a "COPY FROM STDIN" statement using raw data */
  def copyBatchData(sqlStatement: String, dataReader: Reader)
    (implicit connection: Connection) : Unit = {
      val del = new org.apache.commons.dbcp.DelegatingConnection(connection)
      val pg_conn = del.getInnermostDelegate().asInstanceOf[org.postgresql.core.BaseConnection]
      val cm = new org.postgresql.copy.CopyManager(pg_conn)
      cm.copyIn(sqlStatement, dataReader)
      dataReader.close()
    }

}