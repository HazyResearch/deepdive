package org.deepdive.helpers

import org.deepdive.Logging
import org.deepdive.settings._
import java.io._
import scala.sys.process._

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
  
  /** Execute a file as a bash script */
  def executeCmd(cmd: String) : Unit = {
    // Make the file executable, if necessary
    val file = new java.io.File(cmd)
    if (file.isFile) file.setExecutable(true, false)
    log.info(s"""Executing: "$cmd" """)
    val exitValue = cmd!(ProcessLogger(out => log.info(out)))
    // Depending on the exit value we return success or throw an exception
    exitValue match {
      case 0 => 
      case _ => throw new RuntimeException("Script failed")
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
