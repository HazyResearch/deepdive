package org.deepdive.test.unit

import org.deepdive.datastore._
import org.deepdive.settings._
import org.deepdive.Logging
import org.scalatest._
import scala.sys.process._
import scalikejdbc._
import java.io._

class DataUnloader extends FunSpec with BeforeAndAfter with JdbcDataStore {

  lazy implicit val session = DB.autoCommitSession()

  before {
    JdbcDataStore.init()
  }

  after {
    JdbcDataStore.close()
  }

  describe("Unloading data using gpunload") {
    it("should work") {
      // val du = new org.deepdive.datastore.DataUnloader
      // val dbSettings = DbSettings(null, null, null, null, null, null, null, null, null, null)
      // SQL(s"""CREATE TABLE gp(feature text, is_correct boolean, id bigint);""").execute.apply()
      // SQL(s"""INSERT INTO gp values ('hi', true, 0);""").execute.apply()
      // du.gpunload("test_tmp", "select * from gp", dbSettings)
      // val rd = new BufferedReader(new FileReader(s"${dbSettings.gppath}/test_tmp"))
      // var line = rd.readLine()
      // assert(line == "hi\tt\t0")
    }
  }
}