package org.deepdive.test.unit

import com.typesafe.config._
import org.deepdive.datastore._
import org.deepdive.settings._
import org.deepdive.Logging
import org.deepdive.helpers.Helpers
import org.deepdive.test.helpers._
import org.scalatest._
import scala.sys.process._
import scalikejdbc._
import java.io._

class DataLoaderSpec extends FunSpec with BeforeAndAfter with JdbcDataStore {

  lazy implicit val session = DB.autoCommitSession()
  val config = ConfigFactory.parseString(TestHelper.getConfig).withFallback(ConfigFactory.load)

  before {
    JdbcDataStore.init(config)
  }

  after {
    JdbcDataStore.close()
  }

  val dbSettings = TestHelper.getDbSettings

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
      assert(
        (line1 === "hi\tt\t0" && line2 === "\\N\tf\t100") || 
        (line2 === "hi\tt\t0" && line1 === "\\N\tf\t100") || 
        (line1 === "hi\t1\t0" && line2 === "NULL\t0\t100") || 
        (line2 === "hi\t1\t0" && line1 === "NULL\t0\t100"))   // TODO: MySQL now dumps NULL and 0/1
      rd.close()
    }

    it("should work with COPY array types") {
      // MySQL do not have array type
      assume(TestHelper.getTestEnv != TestHelper.Mysql)

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
      SQL(s"""DROP TABLE IF EXISTS dataloader1 CASCADE;""").execute.apply()
      SQL(s"""CREATE TABLE dataloader1(feature text, is_correct boolean, id bigint);""").execute.apply()
      val tsvFile = getClass.getResource("/dataloader1.tsv").getFile
      du.load(new File(tsvFile).getAbsolutePath(), "dataloader1", dbSettings, false)

      // JdbcDataStore.executeSqlQueryWithCallback(sql)(op)
      var result1 = ""
      var result2 = false
      var result3 = ""
      var result4 = 0
      JdbcDataStore.executeSqlQueryWithCallback(s"""SELECT * FROM dataloader1 WHERE id = 0""") { rs =>
        result1 = rs.getString("feature")
        result2 = rs.getBoolean("is_correct")
      }
      JdbcDataStore.executeSqlQueryWithCallback(s"""SELECT * FROM dataloader1 WHERE is_correct = false""") { rs =>
        result3 = rs.getString("feature")
        result4 = rs.getInt("id")
      }
      
//      val result1 = SQL(s"""SELECT * FROM loader WHERE id = 0""").map(rs => rs.string("feature")).single.apply().get 
//      val result2 = SQL(s"""SELECT * FROM loader WHERE id = 0""").map(rs => rs.boolean("is_correct")).single.apply().get 
//      val result3 = SQL(s"""SELECT * FROM loader WHERE is_correct = false""").map(rs => rs.string("feature")).single.apply() 
//      val result4 = SQL(s"""SELECT * FROM loader WHERE is_correct = false""").map(rs => rs.int("id")).single.apply().get 
      
      log.debug(s"DEBUG: GET RESULT ${result1}")
      log.debug(s"DEBUG: GET RESULT ${result2}")
      log.debug(s"DEBUG: GET RESULT ${result3}")
      log.debug(s"DEBUG: GET RESULT ${result4}")

      assert(result1 === "hi")
      assert(result2 === true)
      assert(result3 === null)
      assert(result4 === 100)
    }

    it("""should work with COPY with wildcard in filename""") {
      SQL(s"""DROP TABLE IF EXISTS dataloader1 CASCADE;""").execute.apply()
      SQL(s"""CREATE TABLE dataloader1(feature text, is_correct boolean, id bigint);""").execute.apply()
      val tsvFile = getClass.getResource("/dataloader1.tsv").getFile
      val filePath = new File(tsvFile).getParent() + "/dataloader*.tsv"
      du.load(filePath, "dataloader1", dbSettings, false)
      // val result1 = SQL(s"""SELECT * FROM loader WHERE id = 0""").map(rs => rs.string("feature")).single.apply().get 
      // val result2 = SQL(s"""SELECT * FROM loader WHERE id = 0""").map(rs => rs.boolean("is_correct")).single.apply().get 
      // val result3 = SQL(s"""SELECT * FROM loader WHERE is_correct = false""").map(rs => rs.string("feature")).single.apply() 
      // val result4 = SQL(s"""SELECT * FROM loader WHERE is_correct = false""").map(rs => rs.int("id")).single.apply().get 
      var result1 = ""
      var result2 = false
      var result3 = ""
      var result4 = 0
      JdbcDataStore.executeSqlQueryWithCallback(s"""SELECT * FROM dataloader1 WHERE id = 0""") { rs =>
        result1 = rs.getString("feature")
        result2 = rs.getBoolean("is_correct")
      }
      JdbcDataStore.executeSqlQueryWithCallback(s"""SELECT * FROM dataloader1 WHERE is_correct = false""") { rs =>
        result3 = rs.getString("feature")
        result4 = rs.getInt("id")
      }

      assert(result1 === "hi")
      assert(result2 === true)
      assert(result3 === null)
      assert(result4 === 100)
    }

    it("""should work with gpload""")(pending)

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