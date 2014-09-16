package org.deepdive.helpers

import org.deepdive.Logging
import org.deepdive.settings._
import java.io._
import scala.sys.process._

/** Some helper functions that are used in many places 
 */
object Helpers extends Logging {

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
  def buildPsqlCmd(dbSettings: DbSettings, query: String) : String = {
    // Get Database-related settings
    val dbname = dbSettings.dbname
    val pguser = dbSettings.user
    val pgport = dbSettings.port
    val pghost = dbSettings.host
    // TODO do not use password for now
    val dbnameStr = dbname match {
      case null => ""
      case _ => s" -d ${dbname}"
    }
    val pguserStr = pguser match {
      case null => ""
      case _ => s" -U ${pguser}"
    }
    val pgportStr = pgport match {
      case null => ""
      case _ => s" -p ${pgport}"
    }
    val pghostStr = pghost match {
      case null => ""
      case _ => s" -h ${pghost}"
    }
    List("psql", dbnameStr, pguserStr, pgportStr, pghostStr, " -c ", "\"\"\"", 
      query, "\"\"\"").mkString("")
  }
}
