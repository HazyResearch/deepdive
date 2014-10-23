package org.deepdive.settings
/* Database connection specifie in the settings */
case class DbSettings(driver: String, url: String, user: String, password: String, 
  dbname: String, host: String, port: String, gphost: String, gppath: String, gpport: String)