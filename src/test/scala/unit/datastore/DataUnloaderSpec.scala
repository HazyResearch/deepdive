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

  describe("Unloading data using DataUnloader") {
    it("should work with COPY basic types") {
      val du = new org.deepdive.datastore.DataUnloader
      val dbSettings = DbSettings(null, null, null, null, null, null, null, null, null, null)
      val outputFile = File.createTempFile("test_unloader", "")
      SQL(s"""DROP TABLE IF EXISTS unloader CASCADE;""").execute.apply()
      SQL(s"""CREATE TABLE unloader(feature text, is_correct boolean, id bigint);""").execute.apply()
      SQL(s"""INSERT INTO unloader values ('hi', true, 0), (null, false, 100);""").execute.apply()
      du.unload("test_tmp", s"${outputFile.getAbsolutePath}", dbSettings, false, "select * from unloader;")
      val rd = new BufferedReader(new FileReader(s"${outputFile.getAbsolutePath}"))
      var line = rd.readLine()
      assert(line === "hi\tt\t0")
      line = rd.readLine()
      assert(line === "\\N\tf\t100")
      rd.close()
    }

    it("should work with COPY array types") {
      val du = new org.deepdive.datastore.DataUnloader
      val dbSettings = DbSettings(null, null, null, null, null, null, null, null, null, null)
      val outputFile = File.createTempFile("test_unloader", "")
      SQL(s"""DROP TABLE IF EXISTS unloader CASCADE;""").execute.apply()
      SQL(s"""CREATE TABLE unloader(feature text, id int[]);""").execute.apply()
      SQL(s"""INSERT INTO unloader values ('hi', '{0,1,2}');""").execute.apply()
      du.unload("test_tmp", s"${outputFile.getAbsolutePath}", dbSettings, false, "select * from unloader;")
      val rd = new BufferedReader(new FileReader(s"${outputFile.getAbsolutePath}"))
      var line = rd.readLine()
      assert(line === "hi\t{0,1,2}")
      rd.close()
    }


    it("should work with gpunload")(pending)
    // {
    //   val du = new org.deepdive.datastore.DataUnloader
    //   val dbSettings = DbSettings(null, null, null, null, null, null, null, null, null, null)
    //   val outputFile = File.createTempFile("test_unloader", "")
    //   SQL(s"""CREATE TABLE unloader(feature text, is_correct boolean, id bigint);""").execute.apply()
    //   SQL(s"""INSERT INTO unloader values ('hi', true, 0), (null, false, 100);""").execute.apply()
    //   du.unload("test_tmp", s"${outputFile.getAbsolutePath}", dbSettings, true, "select * from unloader;")
    //   val rd = new BufferedReader(new FileReader(s"${outputFile.getAbsolutePath}"))
    //   var line = rd.readLine()
    //   assert(line === "hi\tt\t0")
    //   line = rd.readLine()
    //   assert(line === "\\N\tf\t100")
    // }
  }
}