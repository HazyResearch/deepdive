package org.deepdive.test.unit

import org.deepdive.Logging
import org.scalatest._
import org.deepdive.helpers.Helpers
import org.deepdive.Context
import org.deepdive.settings._
import java.io._

class HelpersSpec extends FunSpec with BeforeAndAfter {

  describe("Executing bash script using helper function") {
    it("should work") {
      val bashFile = File.createTempFile("test", ".sh")
      val writer = new PrintWriter(bashFile)
      val testPath = s"${Context.outputDir}/test_tmp"
      writer.println(s"mkdir -p ${Context.outputDir} && touch ${testPath}")
      writer.close()

      Helpers.executeCmd(bashFile.getAbsolutePath())
      val testFile = new File(s"${testPath}")
      assert(testFile.exists() === true)

      bashFile.delete()
      testFile.delete()
    }

    it("should throw an error when the script fails") {
      val bashFile = File.createTempFile("test", ".sh")
      val writer = new PrintWriter(bashFile)
      writer.println(s"bad")
      writer.close()
      intercept[RuntimeException] {
        Helpers.executeCmd(bashFile.getAbsolutePath())
      }
      bashFile.delete()
    }

  }

  describe("Slugify") {
    it("should work") {
      assert(Helpers.slugify("simple-string123") == "simple-string123")
      assert(Helpers.slugify("This is the Title of my Blog Post!") == "this-is-the-title-of-my-blog-post")
    }
  }

  describe("Building psql command helper function") {
    it("should work for queries without quotes") {
      val dbSettings = DbSettings(Helpers.PsqlDriver, null, "user", null, "dbname",
        "host", "port", null, null, null, false, IncrementalMode.ORIGINAL)
      val query = "select * from test;"
      val cmd = Helpers.buildSqlCmd(dbSettings, query)
      val trueCmd = "psql -d dbname -U user -p port -h host -v ON_ERROR_STOP=1 -c '" + query + "'"
      assert(cmd.replaceAll(" +", " ").trim() === trueCmd)
    }

    it("should work for queries with single-quotes") {
      val dbSettings = DbSettings(Helpers.PsqlDriver, null, "user", null, "dbname",
        "host", "port", null, null, null, false, IncrementalMode.ORIGINAL)
      val query = "select '123';"
      val cmd = Helpers.buildSqlCmd(dbSettings, query)
      val trueCmd = "psql -d dbname -U user -p port -h host -v ON_ERROR_STOP=1 -c 'select '\\''123'\\'';'"
      assert(cmd.replaceAll(" +", " ").trim() === trueCmd)
    }

    it("should work for queries with double-quotes") {
      val dbSettings = DbSettings(Helpers.PsqlDriver, null, "user", null, "dbname",
        "host", "port", null, null, null, false, IncrementalMode.ORIGINAL)
      val query = "select \"123\";"
      val cmd = Helpers.buildSqlCmd(dbSettings, query)
      val trueCmd = "psql -d dbname -U user -p port -h host -v ON_ERROR_STOP=1 -c 'select \"123\";'"
      assert(cmd.replaceAll(" +", " ").trim() === trueCmd)
    }

  }

}
