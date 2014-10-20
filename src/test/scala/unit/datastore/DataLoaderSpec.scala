package org.deepdive.test.unit

import org.deepdive.datastore._
import org.deepdive.settings._
import org.deepdive.Logging
import org.deepdive.helpers.Helpers
import org.scalatest._
import scala.sys.process._
import scalikejdbc._
import java.io._

class DataLoaderSpec extends FunSpec with BeforeAndAfter with JdbcDataStore {

  lazy implicit val session = DB.autoCommitSession()

  before {
    JdbcDataStore.init()
  }

  after {
    JdbcDataStore.close()
  }

  val dbSettings = DbSettings(Helpers.PsqlDriver, null, System.getenv("PGUSER"), null, System.getenv("DBNAME"), 
    System.getenv("PGHOST"), System.getenv("PGPORT"), System.getenv("GPHOST"), System.getenv("GPPATH"), System.getenv("GPPORT"))

  val du = new org.deepdive.datastore.DataLoader

  describe("Unloading data using DataLoader") {
    it("should work with COPY basic types") {
      val outputFile = File.createTempFile("test_unloader", "")
      SQL(s"""DROP TABLE IF EXISTS unloader CASCADE;""").execute.apply()
      SQL(s"""CREATE TABLE unloader(feature text, is_correct boolean, id bigint);""").execute.apply()
      SQL(s"""INSERT INTO unloader values ('hi', true, 0), (null, false, 100);""").execute.apply()
      du.unload("test_tmp", s"${outputFile.getAbsolutePath}", dbSettings, false, "select * from unloader;")
      val rd = new BufferedReader(new FileReader(s"${outputFile.getAbsolutePath}"))
      val line1 = rd.readLine()
      val line2 = rd.readLine()
      assert((line1 === "hi\tt\t0" && line2 === "\\N\tf\t100") || (line2 === "hi\tt\t0" && line1 === "\\N\tf\t100"))
      rd.close()
    }

    it("should work with COPY array types") {
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


    // it("should work with gpunload") {
    //   val outputFile = File.createTempFile("test_unloader", "")
    //   SQL(s"""DROP TABLE IF EXISTS unloader CASCADE;""").execute.apply()
    //   SQL(s"""CREATE TABLE unloader(feature text, is_correct boolean, id bigint);""").execute.apply()
    //   SQL(s"""INSERT INTO unloader values ('hi', true, 0), (null, false, 100);""").execute.apply()
    //   du.unload("test_tmp", s"${outputFile.getAbsolutePath}", dbSettings, true, "select * from unloader order by id;")
    //   val rd = new BufferedReader(new FileReader(s"${dbSettings.gppath}/test_tmp"))
    //   val line1 = rd.readLine()
    //   val line2 = rd.readLine()
    //   assert((line1 === "hi\tt\t0" && line2 === "\\N\tf\t100") || (line2 === "hi\tt\t0" && line1 === "\\N\tf\t100"))
    //   rd.close()
    // }
  }

  describe("Loading data using DataLoader") {
    it("""should work with COPY""") {
      log.debug("DEBUG start testing!!")
      SQL(s"""DROP TABLE IF EXISTS loader CASCADE;""").execute.apply()
      SQL(s"""CREATE TABLE loader(feature text, is_correct boolean, id bigint);""").execute.apply()
      val tsvFile = getClass.getResource("/dataloader1.tsv").getFile
      du.load(new File(tsvFile).getAbsolutePath(), "loader", dbSettings, false)
      val result1 = SQL(s"""SELECT * FROM loader WHERE id = 0""").map(rs => rs.string("feature")).single.apply().get 
      val result2 = SQL(s"""SELECT * FROM loader WHERE id = 0""").map(rs => rs.boolean("is_correct")).single.apply().get 
      val result3 = SQL(s"""SELECT * FROM loader WHERE is_correct = false""").map(rs => rs.string("feature")).single.apply() 
      val result4 = SQL(s"""SELECT * FROM loader WHERE is_correct = false""").map(rs => rs.int("id")).single.apply().get 

      assert(result1 === "hi")
      assert(result2 === true)
      assert(result3 === None)
      assert(result4 === 100)
    }

    it("""should work with COPY with wildcard in filename""") {
      SQL(s"""DROP TABLE IF EXISTS loader CASCADE;""").execute.apply()
      SQL(s"""CREATE TABLE loader(feature text, is_correct boolean, id bigint);""").execute.apply()
      val tsvFile = getClass.getResource("/dataloader1.tsv").getFile
      val filePath = new File(tsvFile).getParent() + "/dataloader*.tsv"
      du.load(filePath, "loader", dbSettings, false)
      val result1 = SQL(s"""SELECT * FROM loader WHERE id = 0""").map(rs => rs.string("feature")).single.apply().get 
      val result2 = SQL(s"""SELECT * FROM loader WHERE id = 0""").map(rs => rs.boolean("is_correct")).single.apply().get 
      val result3 = SQL(s"""SELECT * FROM loader WHERE is_correct = false""").map(rs => rs.string("feature")).single.apply() 
      val result4 = SQL(s"""SELECT * FROM loader WHERE is_correct = false""").map(rs => rs.int("id")).single.apply().get 

      assert(result1 === "hi")
      assert(result2 === true)
      assert(result3 === None)
      assert(result4 === 100)
    }

    // it("""should work with gpload""") {
    //   SQL(s"""DROP TABLE IF EXISTS loader CASCADE;""").execute.apply()
    //   SQL(s"""CREATE TABLE loader(feature text, is_correct boolean, id bigint);""").execute.apply()
    //   val tsvFile = getClass.getResource("/dataloader1.tsv").getFile
    //   du.load(new File(tsvFile).getAbsolutePath(), "loader", dbSettings, true)
    //   val result1 = SQL(s"""SELECT * FROM loader WHERE id = 0""").map(rs => rs.string("feature")).single.apply().get 
    //   val result2 = SQL(s"""SELECT * FROM loader WHERE id = 0""").map(rs => rs.boolean("is_correct")).single.apply().get 
    //   val result3 = SQL(s"""SELECT * FROM loader WHERE is_correct = false""").map(rs => rs.string("feature")).single.apply() 
    //   val result4 = SQL(s"""SELECT * FROM loader WHERE is_correct = false""").map(rs => rs.int("id")).single.apply().get 

    //   assert(result1 === "hi")
    //   assert(result2 === true)
    //   assert(result3 === None)
    //   assert(result4 === 100)
    // }
  }
}