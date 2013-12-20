package org.deepdive.datastore

import org.deepdive.Logging
import java.sql.Connection
import scalikejdbc.ConnectionPool

/* Helper object for working with Postgres */
object PostgresDataStore extends Logging {

  // Load the driver
  Class.forName("org.postgresql.Driver")
  
  /* Initializes the database. This must be called once before postgres can be used! */
  def init(databaseUrl: String, username: String, password: String) : Unit = {
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

}