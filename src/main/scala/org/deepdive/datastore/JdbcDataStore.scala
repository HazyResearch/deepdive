package org.deepdive.datastore

import java.sql.{Connection, DriverManager, ResultSet, PreparedStatement}
import scalikejdbc._
import scalikejdbc.config._
import org.deepdive.Logging
import com.typesafe.config._
import org.deepdive.helpers.Helpers

trait JdbcDataStore extends Logging {

  def DB = scalikejdbc.DB

  /* Borrows a connection from the connection pool. You should close the connection when done. */
  def borrowConnection() : Connection = ConnectionPool.borrow()

  /* Executes a block with a borrowed connection */
  def withConnection[A](block: Connection => A) = using(ConnectionPool.borrow())(block)

  /* Closes the connection pool and all of its connections */
  def close() = ConnectionPool.closeAll()

  /**
   * Execute any sql query
   * (sql must be only ONE query for mysql, but can be multiple queries for psql.)
   * 
   * @return SQL result set
   */
  def executeSqlQuery(sql: String) = {
    log.debug("Executing single query: " + sql)
    val conn = borrowConnection()
    conn.setAutoCommit(false)
    val stmt = conn.createStatement();
    // Using prepareStatement should be better: faster, prevents SQL injection
    conn.prepareStatement(sql).execute
    // stmt.execute(sql)
    
    conn.commit()
    conn.close()
  }

  /**
   * Issues a single SQL query that can return results, and perform {@code op} 
   * as callback function
   */
  def executeSqlQueryWithCallback(sql: String)
          (op: (java.sql.ResultSet) => Unit) = {
    log.debug("Executing SQL with callback... " + sql)
    val conn = borrowConnection()
    try {
      conn.setAutoCommit(false);
      val stmt = conn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
        java.sql.ResultSet.CONCUR_READ_ONLY);
      stmt.setFetchSize(5000);
      val rs = stmt.executeQuery(sql)
      while(rs.next()){
        op(rs)
      }
    } catch {
      // SQL cmd exception
      case exception : Throwable =>
      log.error(exception.toString)
      throw exception
    } finally {
      conn.close() 
    }
  }
  
 /** 
  *  Execute one or multiple SQL update commands with connection to JDBC datastore
   *  
   */
  def executeSqlQueries(sql: String) : Unit = {
    val conn = borrowConnection()
//    // This commented code can only execute SQL updates that do not return results
//    val stmt = conn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
//      java.sql.ResultSet.CONCUR_UPDATABLE)

    // Supporting more general SQL queries (e.g. SELECT)
    val stmt = conn.createStatement()
    try {
      // changed \s+ to \s* here.
      """;\s*""".r.split(sql.trim()).filterNot(_.isEmpty).foreach(q => {
        log.debug("Executing query via JDBC: " + q.trim())
        // Using prepareStatement should be better: faster, prevents SQL injection
        conn.prepareStatement(q.trim()).execute
      })
    } catch {
      // SQL cmd exception
      case exception : Throwable =>
      log.error(exception.toString)
      throw exception
    } finally {
      conn.close()
    }
  }
  
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


object JdbcDataStore extends JdbcDataStore with Logging {

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
  override def close() = {
    log.info("Closing all JDBC data stores")
    ConnectionPool.closeAll() // TODO not tested
    DBsWithEnv("deepdive").closeAll()
  }
  
}