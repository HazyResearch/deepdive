package org.deepdive.datastore

import org.deepdive.settings._
// import org.deepdive.datastore.JdbcDataStore
import org.deepdive.Logging
import scala.sys.process._

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

      s"rm -f ${path}/${filename}".!

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
      executeQuery(s"COPY (SELECT * FROM _${filename}_view) TO '${filepath}';")
      executeQuery(s"DROP VIEW _${filename}_view;")
    }
  }


}