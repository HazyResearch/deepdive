package org.deepdive.helpers

import org.deepdive.Logging
import org.deepdive.settings._
import org.deepdive.datastore.JdbcDataStore
import java.io._
import scala.sys.process._
import scala.util.{Try, Success, Failure}

/** Some helper functions that are used in many places 
 */
object Helpers extends Logging {
  
  // Constants
  val Psql = "psql"
  val Mysql = "mysql"
    
  val PsqlDriver = "org.postgresql.Driver"
  val MysqlDriver = "com.mysql.jdbc.Driver"
  
  /**
   * Get the dbtype from DbSettings
   * 
   * @returns 
   *     Helpers.Psql 
   *     or
   *     Helpsers.Mysql
   */
  def getDbType(dbSettings: DbSettings) : String = {
    // Branch by database driver type
    dbSettings.driver match {
      case PsqlDriver => Psql
      case MysqlDriver => Mysql
    }    
  }
  
  /**
   * Return a string of psql / mysql options, including user / password / host 
   * / port / database.
   * Do not support password for psql for now.
   * 
   * TODO: using passwords via command line is poor practice. postgres support 
   * specifying password through a PGPASSFILE:
   *   http://www.postgresql.org/docs/current/static/libpq-pgpass.html
   */
  def getOptionString(dbSettings: DbSettings) : String = {
    val dbtype = getDbType(dbSettings)
    val dbname = dbSettings.dbname
    val dbuser = dbSettings.user
    val dbport = dbSettings.port
    val dbhost = dbSettings.host
    val dbpassword = dbSettings.password
      // TODO do not use password for now
    val dbnameStr = dbname match {
      case null => ""
      case _ => dbtype match {
        case Psql => s" -d ${dbname} "
        case Mysql => s" ${dbname} " // can also use -D but mysqlimport does not support -D
      }
    }

    val dbuserStr = dbuser match {
      case null => ""
      case _ => dbtype match {
        case Psql => s" -U ${dbuser} "
        case Mysql => dbpassword match { // see if password is empty
          case null => s" -u ${dbuser} "
          case "" => s" -u ${dbuser} "
          case _ => s" -u ${dbuser} -p=${dbpassword}"
        }
      }
    }
    val dbportStr = dbport match {
      case null => ""
      case _ => dbtype match {
        case Psql => s" -p ${dbport} "
        case Mysql => s" -P ${dbport} "
      }
    }
    val dbhostStr = dbhost match {
      case null => ""
      case _ => s" -h ${dbhost} "
    }
    // Return a concatinated options string
    dbnameStr + dbuserStr + dbportStr + dbhostStr
  }
  
  /** 
   *  Execute a file as a bash script.
   *  
   *  @throws RuntimeException if failed
   */
  def executeCmd(cmd: String) : Unit = {
    // Make the file executable, if necessary
    val file = new java.io.File(cmd)
    if (file.isFile) file.setExecutable(true, false)
    log.info(s"""Executing: "$cmd" """)
    val exitValue = cmd!(ProcessLogger(out => log.info(out)))
    // Depending on the exit value we return success or throw an exception
    exitValue match {
      case 0 => 
      case _ => throw new RuntimeException("Failure when executing script: ${cmd}")
    }
  }


  /**
   * Execute any sql query
   * (sql must be only ONE query for mysql, but can be multiple queries for psql.)
   * 
   * @return SQL result set
   */
  def executeSqlQuery(sql: String, ds: JdbcDataStore) = {
    log.debug("EXECUTING.... " + sql)
    val conn = ds.borrowConnection()
    conn.setAutoCommit(false)
    val stmt = conn.createStatement();
    stmt.execute(sql)
    conn.commit()
    conn.close()
    log.debug("DONE!")
  }

  
 /** 
  *  Execute one or multiple SQL update commands with connection to JDBC datastore
   *  
   */
  def executeSqlUpdates(sql: String, ds: JdbcDataStore) : Unit = {
    val conn = ds.borrowConnection()
    val stmt = conn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
      java.sql.ResultSet.CONCUR_UPDATABLE)
    try {
      // changed \s+ to \s* here.
      """;\s*""".r.split(sql.trim()).filterNot(_.isEmpty).foreach(q => 
        conn.prepareStatement(q.trim()).executeUpdate)
    } catch {
      // SQL cmd exception
      case exception : Throwable =>
      log.error(exception.toString)
      throw exception
    } finally {
      conn.close()
    }
  }

  /** Build a psql command */
  def buildSqlCmd(dbSettings: DbSettings, query: String) : String = {

    // Branch by database driver type
    val dbtype = getDbType(dbSettings)
    
    val sqlQueryPrefix = dbtype match {
      case Psql => "psql " + getOptionString(dbSettings)
      case Mysql => "mysql " + getOptionString(dbSettings)
    }

    // Return the command below
    dbtype match {
      case Psql => sqlQueryPrefix + " -c " + "\"" + query + "\""
      case Mysql => sqlQueryPrefix + " --silent -e " + "\"" + query + "\""
      // TODO what happens if " is in query?
    }
  }
}
