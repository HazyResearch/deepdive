package org.deepdive.datastore

import java.sql.{Connection, DriverManager, ResultSet, PreparedStatement}
import scalikejdbc._
import scalikejdbc.config._
import org.deepdive.Logging
import com.typesafe.config._
import play.api.libs.json._
import org.deepdive.inference.InferenceNamespace

trait JdbcDataStoreComponent {
  def dataStore : JdbcDataStore
}

trait JdbcDataStore extends Logging {

  def DB = scalikejdbc.DB

  /* Borrows a connection from the connection pool. You should close the connection when done. */
  def borrowConnection() : Connection = {
    val c = ConnectionPool.borrow()
    sys.env.get("DEEPDIVE_SCHEMA") match {
      case Some(schema) => c.setSchema(schema)
      case None =>
    } 
    c
  }

  /* Executes a block with a borrowed connection */
  def withConnection[A](block: Connection => A) = using(borrowConnection())(block)

  /* Closes the connection pool and all of its connections */
  def close() = ConnectionPool.closeAll()

  def init() : Unit = {}

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


  def prepareStatement(query: String)
          (op: (java.sql.PreparedStatement) => Unit) = {
    val conn = borrowConnection()
    try {
      conn.setAutoCommit(false)
      val ps = conn.prepareStatement(query)
      op(ps)
      conn.commit()
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
  def executeSqlQueries(sql: String, split: Boolean = true) : Unit = {
    val conn = borrowConnection()
    // Supporting more general SQL queries (e.g. SELECT)
    try {
      if (split) {
        // changed \s+ to \s* here.
        """;\s*""".r.split(sql.trim()).filterNot(_.isEmpty).foreach(q => {
          log.debug("Executing query via JDBC: " + q.trim())
          // Using prepareStatement should be better: faster, prevents SQL injection
          conn.prepareStatement(q.trim()).execute
        })
        } else {
          conn.setAutoCommit(false)
          conn.prepareStatement(sql).execute
          conn.commit()
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

  def bulkInsert(outputRelation: String, data: Iterator[Map[String, Any]])(implicit session: DBSession) = {
    val columnNames = DB.getColumnNames(outputRelation).sorted
    val columnValues = columnNames.map (x => "?")
    val tuples = data.map { tuple =>
      columnNames.map(c => tuple.get(c).orElse(tuple.get(c.toLowerCase)).getOrElse(null))
    }.toSeq
    val conn = borrowConnection()
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
    executeSqlQueries(s"""CREATE ${unlogged} TABLE ${name} (${schema});""")
  }

  /**
   * Drops a table if it exists, and then create it using the given query
   * Ensures we are only dropping tables inside the DeepDive namespace.
   */
  def dropAndCreateTableAs(name: String, query: String) = {
    checkTableNamespace(name)
    executeSqlQueries(s"""DROP TABLE IF EXISTS ${name} CASCADE;""")
    executeSqlQueries(s"""CREATE ${unlogged} TABLE ${name} AS ${query};""")
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

  /**
   * Creates a table if it doesn't exist
   * Ensures we are only creating tables inside the DeepDive namespace.
   */
  def createTableIfNotExists(name: String, schema: String) = {
    checkTableNamespace(name)
    executeSqlQueries(s"""CREATE TABLE IF NOT EXISTS ${name} (${schema});""")
  }

  /**
   * Creates a table if it doesn't exist
   * Ensures we are only creating tables inside the DeepDive namespace.
   */
  def createTableIfNotExistsLike(name: String, source: String) = {
    checkTableNamespace(name)
    executeSqlQueries(s"""CREATE TABLE IF NOT EXISTS ${name} (LIKE ${source});""")
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

  // return if a function of the same name exists in
  // Postgresql or Greenplum
  def existsFunction(function: String) : Boolean = {
    val sql = s"""
      SELECT EXISTS (
        SELECT *
        FROM information_schema.routines
        WHERE routine_name = '${function}'
      );
    """
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

  // check whether postgres-xl is used
  lazy val isUsingPostgresXL : Boolean = {
    var usingXL = false
    executeSqlQueryWithCallback("""SELECT version() LIKE '%Postgres-XL%';""") { rs =>
      usingXL = rs.getBoolean(1)
    }
    usingXL
  }

  def unlogged = if (isUsingPostgresXL) "UNLOGGED" else ""

  // ========================================
  // Extraction

  def queryAsMap[A](query: String, batchSize: Option[Int] = None)
    (block: Iterator[Map[String, Any]] => A) : A = {
    DB.readOnly { implicit session =>
      session.connection.setAutoCommit(false)
      val stmt = session.connection.createStatement(
        java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY)
      stmt.setFetchSize(10000)
      try {
        // stmt.executeUpdate("ANALYZE");
        log.debug(query)
        val expQuery = "EXPLAIN " + query
        val ex = stmt.executeQuery(expQuery)
        log.debug(ex.getMetaData().getColumnLabel(1))
        while (ex.next()) {
          log.debug(ex getString 1)
        }

        val rs = stmt.executeQuery(query)
        // No result return
        if (!rs.isBeforeFirst) {
          log.warning(s"query returned no results: ${query}")
          block(Iterator.empty)
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
              }
              .toMap
            }
          }
          block(resultIter)
        }
      } catch {
        // SQL cmd exception
        case exception : Throwable =>
          log.error(exception.toString)
          throw exception
      }
    }
  }

  def queryAsJson[A](query: String, batchSize: Option[Int] = None)
    (block: Iterator[JsObject] => A) : A = {
    queryAsMap(query, batchSize) { iter =>
      val jsonIter = iter.map { row =>
        JsObject(row.mapValues(anyValToJson).toSeq)
      }
      block(jsonIter)
    }
  }

  def queryUpdate(query: String) {
    val conn = borrowConnection()
    //conn.setAutoCommit(false);
    try {
      val prep = conn.prepareStatement(query)
      prep.executeUpdate
    } catch {
      // SQL cmd exception
      case exception : Throwable =>
        log.error(exception.toString)
        throw exception
    } finally {
      conn.close()
    }
  }

  /* Translates an arbitary values that comes back from the database to a JSON value */
  def anyValToJson(x: Any) : JsValue = x match {
    case Some(x) => anyValToJson(x)
    case None | null => JsNull
    case x : String => JsString(x)
    case x : Boolean => JsBoolean(x)
    case x : Int => JsNumber(x)
    case x : Long => JsNumber(x)
    case x : Float => JsNumber(x)
    case x : Double => JsNumber(x)
    case x : java.sql.Date => JsString(x.toString)
    case x : Array[_] => JsArray(x.toList.map(x => anyValToJson(x)))
    case x : List[_] => JsArray(x.toList.map(x => anyValToJson(x)))
    case x : JsObject => x      case x =>
      log.error(s"Could not convert ${x.toString} of type=${x.getClass.getName} to JSON")
      JsNull
  }

  def addBatch(result: Iterator[JsObject], outputRelation: String) : Unit = {}

  // Datastore-specific methods:
  // Below are methods to implement in any type of datastore.

  /**
   * Drop and create a sequence, based on database type.
   *
   * @see http://dev.mysql.com/doc/refman/5.0/en/user-variables.html
   * @see http://www.it-iss.com/mysql/mysql-renumber-field-values/
   */
  def createSequenceFunction(seqName: String) : String = null

  /**
   * Cast an expression to a type
   */
  def cast(expr: Any, toType: String): String = null

  /**
   * Given a string column name, Get a quoted version dependent on DB.
   *          if psql, return "column"
   *          if mysql, return `column`
   */
  def quoteColumn(column: String) : String = null

  /**
   * Generate a random real number from 0 to 1.
   */
  def randomFunction : String = null

  /**
   * Concatenate a list of strings in the database.
   * @param list
   *     the list to concat
   * @param delimiter
   *     the delimiter used to seperate elements
   * @return
   *   Use '||' in psql, use 'concat' function in mysql
   */
  def concat(list: Seq[String], delimiter: String) : String = null

  // fast sequential id assign function
  def createSpecialUDFs() : Unit = {}

  /**
   * ANALYZE TABLE
   */
  def analyzeTable(table: String) : String = ""

  // assign sequential ids to table's id column
  def assignIds(table: String, startId: Long, sequence: String) : Long = 0

  // check if a table exists
  def existsTable(table: String) : Boolean = false;

  // assign sequential ids in particular order
  def assignIdsOrdered(table: String, startId: Long, sequence: String, orderBy: String = "") : Long = throw new UnsupportedOperationException

  // end: Datastore-specific methods

}

object JdbcDataStoreObject extends JdbcDataStore with Logging {

  class JdbcDBsWithEnv(envValue: String, configObj: Config) extends DBsWithEnv(envValue) {
    override lazy val config = configObj.withValue("deepdive.db.default.poolFactoryName",
      ConfigValueFactory.fromAnyRef("commons-dbcp2"))
  }

  /* Initializes the data stores */
  def init(config: Config) : Unit = {
    val initializer = new JdbcDBsWithEnv("deepdive", config)
    log.info("Intializing all JDBC data stores")
    initializer.setupAll()
  }

  override def init() : Unit = init(ConfigFactory.load)

  /* Closes the data store */
  override def close() = {
    log.info("Closing all JDBC data stores")
    ConnectionPool.closeAll() // TODO not tested
    DBsWithEnv("deepdive").closeAll()
  }

}
