package org.deepdive.datastore

import org.deepdive.settings._
// import org.deepdive.datastore.JdbcDataStore
import org.deepdive.Logging
import org.deepdive.helpers.Helpers
import scala.sys.process._
import scala.util.{Try, Success, Failure}
import java.io._

class DataLoader extends JdbcDataStore with Logging {

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

      if (path != "" && filename != "" && hostname != "") {
        new File(s"${path}/${filename}").delete()
      } else {
        throw new RuntimeException("greenplum parameters gphost, gpport, gppath are not set!")
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

      executeQuery(s"""
        DROP VIEW IF EXISTS _${filename}_view CASCADE;
        CREATE VIEW _${filename}_view AS ${query};
        """)

      val outfile = new File(filepath)
      outfile.getParentFile().mkdirs()

      val cmdfile = File.createTempFile(s"copy", ".sh")
      val writer = new PrintWriter(cmdfile)
      val sql = """\COPY """ + s"(SELECT * FROM _${filename}_view) TO '${filepath}';"
      val copyStr = Helpers.buildPsqlCmd(dbSettings, sql)
      log.info(copyStr)
      writer.println(copyStr)
      writer.close()
      Helpers.executeCmd(cmdfile.getAbsolutePath())
      executeQuery(s"DROP VIEW _${filename}_view;")
      cmdfile.delete()
    }
  }

  /** Load data from a file to database
   *
   * For greenplum, use gpload; for postgres, use \copy
   * @delimter: the single character that separates columns within each row (line) of the file.
   * @filepath: the absolute path of the input file
   */ 
  def load(filepath: String, tablename: String, dbSettings: DbSettings, delimiter: String, usingGreenplum: Boolean) : Unit = {
    if (usingGreenplum) {
    } else {
      val cmdfile = File.createTempFile(s"copy", ".sh")
      val writer = new PrintWriter(cmdfile)
      val sql = """\COPY """ + s"${tablename} FROM ${filepath} DELIMITER ${delimiter}"
      val copyStr = Helpers.buildPsqlCmd(dbSettings, sql)
      log.info(copyStr)
      writer.println(copyStr)
      writer.close()
      Helpers.executeCmd(cmdfile.getAbsolutePath())
      cmdfile.delete()
    }
  }


}