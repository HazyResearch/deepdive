package org.deepdive.datastore

import java.sql.{Connection, DriverManager, ResultSet, PreparedStatement}
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
    val columnNames = PostgresDataStore.DB.getColumnNames(outputRelation).sorted
    val columnValues = columnNames.map (x => "?")
    val tuples = data.map { tuple =>
      columnNames.map(c => tuple.get(c).orElse(tuple.get(c.toLowerCase)).getOrElse(null))
    }.toSeq
    val conn = ConnectionPool.borrow()
    val stmt = conn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
      java.sql.ResultSet.CONCUR_UPDATABLE)
    val ps = conn.prepareStatement(s"""INSERT INTO ${outputRelation} (${columnNames.mkString(", ")}) 
      VALUES (${columnValues.mkString(", ")})""")
    try {
      for (tuple <- tuples) {
        for((value, index) <- tuple.view.zipWithIndex) {
          value match {
            case z:Boolean => ps.setBoolean(index + 1, z) 
            case z:Byte => ps.setByte(index + 1, z)
            case z:Int => ps.setInt(index + 1, z)
            case z:Long => ps.setLong(index + 1, z)
            case z:Float => ps.setFloat(index + 1, z)
            case z:Double => ps.setDouble(index + 1, z)
            case z:String => ps.setString(index + 1, z)
            //case z:Date => ps.setDate(index + 1, z)
            case z => ps.setObject(index + 1, z)
          }
        }
        ps.addBatch()
      }
      ps.executeBatch()
    } catch {
      // SQL cmd exception
      case exception : Throwable =>
      log.error(exception.toString)
      throw exception
    } finally {
      conn.close()
    }
  }
}


object JdbcDataStore extends Logging {

  def executeCmd(cmd: String) {
    val conn = ConnectionPool.borrow()
    val stmt = conn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_UPDATABLE)
    try {
      var prep = null
      """;\s+""".r.split(cmd.trim()).filterNot(_.isEmpty).foreach(q => conn.prepareStatement(q.trim()).executeUpdate)
    } catch {
      // SQL cmd exception
      case exception : Throwable =>
      log.error(exception.toString)
      throw exception
    } finally {
      conn.close()
    }
  }

  class JdbcDBsWithEnv(envValue: String, configObj: Config) extends DBsWithEnv(envValue) {
    override lazy val config = configObj
  }

  /* Initializes the data stores */
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