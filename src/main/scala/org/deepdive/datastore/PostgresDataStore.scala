package org.deepdive.datastore

import java.sql.Connection
import scalikejdbc.ConnectionPool

object PostgresDataStore {

  Class.forName("org.postgresql.Driver")
  ConnectionPool.singleton("jdbc:postgresql://localhost/deepdive_paleo", "dennybritz", "")

  def withConnection[A](block: Connection => A): A = {
    val connection: Connection = ConnectionPool.borrow()
    try {
      block(connection)
    } finally {
      connection.close()
    }
  }

}