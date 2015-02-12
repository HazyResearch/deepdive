package org.deepdive.datastore

import java.sql.{Connection, DriverManager, ResultSet, PreparedStatement}
import scalikejdbc._
import scalikejdbc.config._
import org.deepdive.Logging
import com.typesafe.config._
import org.deepdive.helpers.Helpers
import play.api.libs.json._
import org.deepdive.inference.InferenceNamespace

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

  // check if the given table name is deepdive's internal table, if not throw exception
  def checkTableNamespace(name: String) = {
    if (!name.startsWith(InferenceNamespace.deepdivePrefix)) {
      throw new RuntimeException("Dropping a non-deepdive internal table!")
    }
  }
  
  /**
   * Drops a table if it exists, and then create it
   * Ensures we are only dropping tables inside the DeepDive namespace.
   */
  def dropAndCreateTable(name: String, schema: String) = {
    checkTableNamespace(name)
    executeSqlQueries(s"""DROP TABLE IF EXISTS ${name} CASCADE;""")
    executeSqlQueries(s"""CREATE TABLE ${name} (${schema});""")
  }

  /**
   * Drops a table if it exists, and then create it using the given query
   * Ensures we are only dropping tables inside the DeepDive namespace.
   */
  def dropAndCreateTableAs(name: String, query: String) = {
    checkTableNamespace(name)
    executeSqlQueries(s"""DROP TABLE IF EXISTS ${name} CASCADE;""")
    executeSqlQueries(s"""CREATE TABLE ${name} AS ${query};""")
  }

  // execute sql, store results in a map
  def selectAsMap(sql: String) : List[Map[String, Any]] = {
    val conn = borrowConnection()
    conn.setAutoCommit(false)
    try {
      val stmt = conn.createStatement(
        java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY)
      stmt.setFetchSize(10000)
      val rs = stmt.executeQuery(sql)
      // No result return
      if (!rs.isBeforeFirst) {
        log.warning(s"query returned no results: ${sql}")
        Iterator.empty.toSeq
      } else {
        val resultIter = new Iterator[Map[String, Any]] {
          def hasNext = {
            // TODO: This is expensive
            !(rs.isLast)
          }              
          def next() = {
            rs.next()
            val metadata = rs.getMetaData()
            (1 to metadata.getColumnCount()).map { i => 
              val label = metadata.getColumnLabel(i)
              val data = unwrapSQLType(rs.getObject(i))
              (label, data)
            }.filter(_._2 != null).toMap
          }
        }
        resultIter.toSeq
      }
    } catch {
      // SQL cmd exception
      case exception : Throwable =>
        log.error(exception.toString)
        throw exception
    } finally {
      conn.close()
    }


    DB.readOnly { implicit session =>
      SQL(sql).map(_.toMap).list.apply()
    }
  }

  private def unwrapSQLType(x: Any) : Any = {
    x match {
      case x : org.postgresql.jdbc4.Jdbc4Array => x.getArray().asInstanceOf[Array[_]].toList
      case x : org.postgresql.util.PGobject =>
        x.getType match {
          case "json" => Json.parse(x.getValue)
          case _ => JsNull
        }
      case x => x
    }
  }

  // returns if a language exists in Postgresql or Greenplum
  def existsLanguage(language: String) : Boolean = {
    val sql = s"""SELECT EXISTS (
      SELECT 1
      FROM   pg_language
      WHERE  lanname = '${language}');"""
    var exists = false
    executeSqlQueryWithCallback(sql) { rs =>
      exists = rs.getBoolean(1)
    }
    return exists
  }

  // check whether greenplum is used
  def isUsingGreenplum() : Boolean = {
    var usingGreenplum = false
    executeSqlQueryWithCallback("""SELECT version() LIKE '%Greenplum%';""") { rs => 
      usingGreenplum = rs.getBoolean(1) 
    }
    return usingGreenplum
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
    // create language for and greenplum if not exist
    if (isUsingGreenplum()) {
      if (!existsLanguage("plpgsql")) {
        executeSqlQueries("CREATE LANGUAGE plpgsql;")
      }
      if (!existsLanguage("plpythonu")) {
        executeSqlQueries("CREATE LANGUAGE plpythonu;")
      }
    }
  }

  def init() : Unit = init(ConfigFactory.load)

  /* Closes the data store */
  override def close() = {
    log.info("Closing all JDBC data stores")
    ConnectionPool.closeAll() // TODO not tested
    DBsWithEnv("deepdive").closeAll()
  }
  
}