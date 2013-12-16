package org.deepdive.datastore

import org.deepdive.Logging
import java.sql.Connection
import scalikejdbc.ConnectionPool

object PostgresDataStore extends Logging {

  Class.forName("org.postgresql.Driver")
  
  def init(databaseUrl: String, username: String, password: String) : Unit = {
    ConnectionPool.singleton(databaseUrl, username, password)
    log.info(s"Initialized postgres datastore at url=${databaseUrl}")
  }

  def borrowConnection() : Connection = ConnectionPool.borrow()

  def withConnection[A](block: Connection => A): A = {
    val connection: Connection = ConnectionPool.borrow()
    try {
      block(connection)
    } finally {
      connection.close()
    }
  }

}