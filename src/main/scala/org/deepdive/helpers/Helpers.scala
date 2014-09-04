package org.deepdive.helpers

import org.deepdive.Logging
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
}
