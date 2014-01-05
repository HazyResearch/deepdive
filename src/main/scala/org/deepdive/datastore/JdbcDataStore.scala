package org.deepdive.datastore

import java.sql.Connection
import scalikejdbc._
import scalikejdbc.config._
import org.deepdive.Logging
import com.typesafe.config._

trait JdbcDataStore {

  /* Borrows a connection from the connection pool. You should close the connection when done. */
  def borrowConnection() : Connection = ConnectionPool.borrow()

  /* Executes a block with a borrowed connection */
  def withConnection[A](block: Connection => A) = using(ConnectionPool.borrow())(block)

  /* Closes the connection pool and all of its connections */
  def close() = ConnectionPool.closeAll()

}

object JdbcDataStore extends Logging {

  class JdbcDBsWithEnv(envValue: String, configObj: Config) extends DBsWithEnv(envValue) {
    override lazy val config = configObj
  }

  /* Initializes the data store */
  def init(config: Config) : Unit = {
    val initializer = new JdbcDBsWithEnv("deepdive", config)
    log.info("Intializing all JDBC data stores")
    initializer.setupAll()
  }

  def init() : Unit = init(ConfigFactory.load)

  /* Closes the data store */
  def close() = {
    log.info("Closing all JDBC data stores")
    DBsWithEnv("deepdive").closeAll()
  }

}