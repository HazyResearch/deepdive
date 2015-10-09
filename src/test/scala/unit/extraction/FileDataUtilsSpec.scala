package org.deepdive.test.unit

import java.io.File
import org.scalatest._
import org.deepdive.datastore._
import play.api.libs.json._

class FileDataUtilsSpec extends FunSpec {

  val sampleCSVFile = getClass.getResource("/sample.csv").getFile

  describe("Recursively listing a directory") {
    it("should work") {
      val dirPath = getClass.getResource("/files/source").getPath
      val files = FileDataUtils.recursiveListFiles(new File(dirPath))
      assert(files.length == 4)
      assert(files.exists{ f =>
        f.toString.endsWith("convert.py")
      })
      assert(files.exists{ g =>
        g.toString.endsWith("subdir")
      })
    }
  }

  describe("Zipping a directory") {
    it("should work with subdirectories, empty files, and binary files") {
      val dirPath = getClass.getResource("/files/source").getPath
      val blob = FileDataUtils.zipDir(dirPath)
      assert(blob.length == 749)
      // TODO: verify zip entries
    }
  }

}
