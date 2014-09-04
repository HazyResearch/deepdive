package org.deepdive.test.unit

import org.deepdive.Logging
import org.scalatest._
import org.deepdive.helpers.Helpers
import org.deepdive.Context
import java.io._

class HelpersSpec extends FunSpec with BeforeAndAfter {

  describe("Executing bash script using helper function") {
    it("should work") {
      val bashFile = File.createTempFile("test", ".sh")
      val writer = new PrintWriter(bashFile)
      val testPath = s"${Context.outputDir}/test_tmp}"
      writer.println(s"touch ${testPath}")
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

}