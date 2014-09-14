package org.deepdive.datastore

import org.deepdive.settings._
// import org.deepdive.datastore.JdbcDataStore
import org.deepdive.Logging
import scala.sys.process._
import scala.util.{Try, Success, Failure}
import java.io._

class DataUnloader extends JdbcDataStore with Logging {

  // def ds : JdbcDataStore

  /**
   * Can execute multiple queries
   */
  def executeQuery(sql: String) = {
    log.debug("EXECUTING.... " + sql)
    val conn = borrowConnection()
    conn.setAutoCommit(false)
    val stmt = conn.createStatement();
    try {
      """;\s*""".r.split(sql.trim()).filterNot(_.isEmpty).foreach(q =>
        stmt.execute(q.trim()))
      conn.commit()
      log.debug("DONE!")
      
    } finally {
      conn.close()
    }

  }
  
//  def executeQuery(sql: String) = {
//    log.debug("EXECUTING.... " + sql)
//    val conn = borrowConnection()
//    conn.setAutoCommit(false)
//    val stmt = conn.createStatement();
//    try {
//      stmt.execute(sql)
//      conn.commit()
//      log.debug("DONE!")
//    } finally {
//      conn.close()
//    }
//  }

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

  /** Unload data from database to a file 
   * 
   * For Greenplum, use gpfdist. Must specify gpport, gppath, gphost in dbSettings. No need for filepath
   * For Postgresql, filepath is an abosulute path. No need for dbSettings or filename.
   */
  def unload(filename: String, filepath: String, dbSettings: DbSettings, usingGreenplum: Boolean, query: String) : Unit = {
    
    if (usingGreenplum) {
      val hostname = dbSettings.gphost
      val port = dbSettings.gpport
      val path = dbSettings.gppath

      if(path != "" && filename != ""){
        s"rm -f ${path}/${filename}".!
      }else{
        // TODO: die
      }

      // hacky way to get schema from a query...
      executeQuery(s"""
        DROP VIEW IF EXISTS _${filename}_view CASCADE;
        DROP TABLE IF EXISTS _${filename}_tmp CASCADE;
        CREATE VIEW _${filename}_view AS ${query};
        CREATE TABLE _${filename}_tmp AS SELECT * FROM _${filename}_view LIMIT 0;
        """)

      executeQuery(s"""
        DROP EXTERNAL TABLE IF EXISTS _${filename} CASCADE;
        CREATE WRITABLE EXTERNAL TABLE _${filename} (LIKE _${filename}_tmp)
        LOCATION ('gpfdist://${hostname}:${port}/${filename}')
        FORMAT 'TEXT';
        """)

      executeQuery(s"""
        DROP VIEW _${filename}_view CASCADE;
        DROP TABLE _${filename}_tmp CASCADE;""")

      executeQuery(s"""
        INSERT INTO _${filename} ${query};
        """)
    } else { // postgresql / mariadb
      
      // Branch by database driver type (temporary solution)
      val dbtype = dbSettings.driver match {
        case "org.postgresql.Driver" => "psql"
        case "com.mysql.jdbc.Driver" => "mysql"
      }
      // Generate SQL query prefixes
      val dbname = dbSettings.dbname
      val dbuser = dbSettings.user
      val dbport = dbSettings.port
      val dbhost = dbSettings.host
      val dbpassword = dbSettings.password
      // TODO do not use password for now
      val dbnameStr = dbname match {
        case null => ""
        case _ => dbtype match {
          case "psql" => s" -d ${dbname} "
          case "mysql" => s" ${dbname} " // can also use -D but mysqlimport does not support -D
        }
      }

      val dbuserStr = dbuser match {
        case null => ""
        case _ => dbtype match {
          case "psql" => s" -U ${dbuser} "
          case "mysql" => dbpassword match { // see if password is empty
            case null => s" -u ${dbuser} "
            case "" => s" -u ${dbuser} "
            case _ => s" -u ${dbuser} -p=${dbpassword}"
          }
        }
      }
      val dbportStr = dbport match {
        case null => ""
        case _ => dbtype match {
          case "psql" => s" -p ${dbport} "
          case "mysql" => s" -P ${dbport} "
        }
      }
      val dbhostStr = dbhost match {
        case null => ""
        case _ => s" -h ${dbhost} "
      }
      val sqlQueryPrefixRun = dbtype match {
        case "psql" => "psql " + dbnameStr + dbuserStr + dbportStr + dbhostStr + " -c "
        case "mysql" => "mysql " + dbnameStr + dbuserStr + dbportStr + dbhostStr + " --silent -e "
      }
  
      executeQuery(s"""
        DROP VIEW IF EXISTS _${filename}_view CASCADE;
        CREATE VIEW _${filename}_view AS ${query};
        """)

      val outfile = new File(filepath)
      outfile.getParentFile().mkdirs()

      val cmdfile = File.createTempFile(s"copy", ".sh")
      val writer = new PrintWriter(cmdfile)
      
      val copyStr = dbtype match {
        case "psql" => List(sqlQueryPrefixRun + "\"", 
            """\COPY """, s"(SELECT * FROM _${filename}_view) TO '${filepath}'", "\"").mkString("")
            
        case "mysql" => List(sqlQueryPrefixRun + "\"", 
            s"SELECT * FROM _${filename}_view", "\"", s"> ${filepath}").mkString("")
      }
        
      log.info(copyStr)
      writer.println(copyStr)
      writer.close()
      executeCmd(cmdfile.getAbsolutePath())
      // executeQuery(s"""COPY (SELECT * FROM _${filename}_view) TO '${filepath}';""")
      executeQuery(s"DROP VIEW _${filename}_view;")
      s"rm ${cmdfile.getAbsolutePath()}".!
    }
  }


}
