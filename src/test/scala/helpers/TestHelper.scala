package org.deepdive.test.helpers

import org.deepdive.settings.DbSettings
import org.deepdive.helpers._

object TestHelper {
  // Constants
  val Psql = "psql"
  val Mysql = "mysql"
  val Greenplum = "greenplum"

  def getTestEnv() = System.getenv("DEEPDIVE_TEST_ENV") match {
    case null => Psql
    case "psql" => Psql
    case "mysql" => Mysql
    // Do not have tests for GP right now.
    case "greenplum" => Greenplum 
  }

  def getDriverFromEnv() = System.getenv("DEEPDIVE_TEST_ENV") match {
    case null | Psql | Greenplum => "org.postgresql.Driver"
    case Mysql => "com.mysql.jdbc.Driver"
  }

  def getDbSettings() = 
      DbSettings(getDriverFromEnv,      // driver 
          System.getenv("DBCONNSTRING"),  // nrl
          System.getenv("DBUSER"),        // user
          System.getenv("DBPASSWORD"),    // password
          System.getenv("DBNAME"),        // dbname
          System.getenv("DBHOST"), 
          System.getenv("DBPORT"), 
          System.getenv("GPHOST"), 
          System.getenv("GPPATH"), 
          System.getenv("GPPORT"))
    
  def getConfig() = s"""
      deepdive.db.default {
        driver: ${TestHelper.getDriverFromEnv()}
        url: "${System.getenv("DBCONNSTRING")}"
        user: "${System.getenv("DBUSER")}"
        password: "${System.getenv("DBPASSWORD")}"
        dbname: "${System.getenv("DBNAME")}"
        host: "${System.getenv("DBHOST")}"
        port: "${System.getenv("DBPORT")}"
        gphost: "${System.getenv("GPHOST")}"
        gppath: "${System.getenv("GPPATH")}"
        gpport: "${System.getenv("GPPORT")}"
      }
    """
  
  

}