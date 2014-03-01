package org.deepdive.datastore

import java.sql.Connection
import scalikejdbc._
import scalikejdbc.config._
import org.deepdive.Logging
import com.typesafe.config._

trait JdbcDataStore extends Logging {

  def DB = scalikejdbc.DB

  /* Borrows a connection from the connection pool. You should close the connection when done. */
  def borrowConnection() : Connection = ConnectionPool.borrow()

  /* Executes a block with a borrowed connection */
  def withConnection[A](block: Connection => A) = using(ConnectionPool.borrow())(block)

  /* Closes the connection pool and all of its connections */
  def close() = ConnectionPool.closeAll()

  def bulkInsert(outputRelation: String, data: Iterator[Map[String, Any]])(implicit session: DBSession) = {
    val columnNames = HSQLDataStore.DB.getColumnNames(outputRelation).sorted
    val columnValues = columnNames.map (x => "?")
    val copySQL = s"""INSERT INTO ${outputRelation}(${columnNames.mkString(", ")}) 
      VALUES (${columnValues.mkString(", ")})"""
    val tuples = data.map { tuple =>
      columnNames.map(c => tuple.get(c).orElse(tuple.get(c.toLowerCase)).getOrElse(null))
    }.toSeq
    log.debug(s"copying num_records=${tuples.size} into relation=${outputRelation}")
    SQL(copySQL).batch(tuples: _*).apply()
  }

}

object JdbcDataStore extends Logging {

  def executeCmd(cmd: String) : Unit = {
    DB.autoCommit { implicit session =>
      """;\s+""".r.split(cmd.trim()).filterNot(_.isEmpty).foreach(q => SQL(q.trim()).execute.apply())
    }
  }

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