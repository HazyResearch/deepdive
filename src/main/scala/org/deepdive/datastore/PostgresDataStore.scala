package org.deepdive.datastore

import java.io.{File, Reader, FileReader, InputStream, InputStreamReader}
import java.sql.Connection
import java.util.concurrent.atomic.AtomicLong
import org.deepdive.Logging
import scalikejdbc.ConnectionPool

/* Helper object for working with Postgres */
object PostgresDataStore extends Logging {

  // Load the driver
  Class.forName("org.postgresql.Driver")

  private val variableIdCounter = new AtomicLong()
  
  /* Initializes the database. This must be called once before postgres can be used! */
  def init(databaseUrl: String, username: String, password: String) : Unit = {
    variableIdCounter.set(0)
    ConnectionPool.singleton(databaseUrl, username, password)
    log.info(s"Initialized postgres datastore at url=${databaseUrl}")
  }

  /* Borrows a connection from the connection pool. You should close the connection when done. */
  def borrowConnection() : Connection = ConnectionPool.borrow()

  /* Executes a block with a borrowed connection */
  def withConnection[A](block: Connection => A): A = {
    val connection: Connection = ConnectionPool.borrow()
    try {
      block(connection)
    } finally {
      connection.close()
    }
  }

  /* Closes the connection pool and all of its connections */
  def close() = ConnectionPool.closeAll()

  /* Returns the next globally unique id and increases the id counter by one */
  def nextId() = variableIdCounter.getAndIncrement()

  def currentId = variableIdCounter.get()

  def copyBatchData(sqlStatement: String, file: File)(implicit connection: Connection) : Unit = {
    copyBatchData(sqlStatement, new FileReader(file)) 
  }

  def copyBatchData(sqlStatement: String, rawData: InputStream)
    (implicit connection: Connection) : Unit = {
    copyBatchData(sqlStatement, new InputStreamReader(rawData)) 
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