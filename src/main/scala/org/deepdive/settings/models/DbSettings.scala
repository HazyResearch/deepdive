package org.deepdive.settings

object IncrementalMode extends Enumeration {
  type IncrementalMode = Value
  // INCREMENTAL => Generate incremental application.conf
  // MERGE => Merge new generated data into original table
  val ORIGINAL, INCREMENTAL, MATERIALIZATION = Value
}

/* Database connection specifie in the settings */
case class DbSettings(driver: String, url: String, user: String, password: String, 
  dbname: String, host: String, port: String, gphost: String, gppath: String, 
  gpport: String, gpload: Boolean, incrementalMode: IncrementalMode.IncrementalMode, 
  keyMap: Map[String, List[String]] = null)

