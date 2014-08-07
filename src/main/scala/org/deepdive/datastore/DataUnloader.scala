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

  // gpunload
  def gpunload(filename: String, query: String, dbSettings: DbSettings) : Unit = {

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
  }


}