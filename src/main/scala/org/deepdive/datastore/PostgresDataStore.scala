package org.deepdive.datastore

import java.sql.Connection
import scalikejdbc.ConnectionPool

object PostgresDataStore {

  Class.forName("org.postgresql.Driver")
  
  def init(connectionStr: String, username: String, password: String) {
    ConnectionPool.singleton(connectionStr, username, password)
  }

  def borrowConnection() = ConnectionPool.borrow()

  def withConnection[A](block: Connection => A): A = {
    val connection: Connection = ConnectionPool.borrow()
    try {
      block(connection)
    } finally {
      connection.close()
    }
  }

}