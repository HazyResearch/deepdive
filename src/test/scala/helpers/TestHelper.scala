package org.deepdive.test.helpers

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
    case null => "org.postgresql.Driver"
    case "psql" => "org.postgresql.Driver"
    case "mysql" => "com.mysql.jdbc.Driver"
    case "greenplum" => "org.postgresql.Driver"
  }

}