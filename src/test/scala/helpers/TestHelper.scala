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
}