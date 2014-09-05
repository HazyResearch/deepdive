package org.deepdive.datastore

import org.deepdive.settings._
// import org.deepdive.datastore.JdbcDataStore
import org.deepdive.Logging
import scala.sys.process._
import scala.util.{Try, Success, Failure}
import java.io._

class DataUnloader extends JdbcDataStore with Logging {

  // def ds : JdbcDataStore

  def executeQuery(sql: String) = {
    log.debug("EXECUTING.... " + sql)
    val conn = borrowConnection()
    conn.setAutoCommit(false)
    val stmt = conn.createStatement();
    stmt.execute(sql)
    conn.commit()
    conn.close()  
    log.debug("DONE!")
  }

  def executeCmd(cmd: String) : Try[Int] = {
    // Make the file executable, if necessary
    val file = new java.io.File(cmd)
    if (file.isFile) file.setExecutable(true, false)
    log.info(s"""Executing: "$cmd" """)
    val processLogger = ProcessLogger(line => log.info(line))
    Try(cmd!(processLogger)) match {
      case Success(0) => Success(0)
      case Success(errorExitValue) => 
        Failure(new RuntimeException(s"Script exited with exit_value=$errorExitValue"))
      case Failure(ex) => Failure(ex)
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
    } else {
      // Get Database-related settings
      val dbname = dbSettings.dbname
      val pguser = dbSettings.user
      val pgport = dbSettings.port
      val pghost = dbSettings.host
      // TODO do not use password for now
      val dbnameStr = dbname match {
        case null => ""
        case _ => s" -d ${dbname} "
      }
      val pguserStr = pguser match {
        case null => ""
        case _ => s" -U ${pguser} "
      }
      val pgportStr = pgport match {
        case null => ""
        case _ => s" -p ${pgport} "
      }
      val pghostStr = pghost match {
        case null => ""
        case _ => s" -h ${pghost} "
      }
      executeQuery(s"""
        DROP VIEW IF EXISTS _${filename}_view CASCADE;
        CREATE VIEW _${filename}_view AS ${query};
        """)

      val outfile = new File(filepath)
      outfile.getParentFile().mkdirs()

      val cmdfile = File.createTempFile(s"copy", ".sh")
      val writer = new PrintWriter(cmdfile)
      val copyStr = List("psql ", dbnameStr, pguserStr, pgportStr, pghostStr, " -c ", "\"\"\"", 
        """\COPY """, s"(SELECT * FROM _${filename}_view) TO '${filepath}'", "\"\"\"").mkString("")
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
