package org.deepdive.datastore

import java.sql.Connection
import scalikejdbc._
import org.deepdive.Logging

trait JdbcDataStore {

  def init() : Unit

  /* Borrows a connection from the connection pool. You should close the connection when done. */
  def borrowConnection() : Connection = ConnectionPool.borrow()

  /* Executes a block with a borrowed connection */
  def withConnection[A](block: Connection => A) = using(ConnectionPool.borrow())(block)

  /* Closes the connection pool and all of its connections */
  def close() = ConnectionPool.closeAll()

}