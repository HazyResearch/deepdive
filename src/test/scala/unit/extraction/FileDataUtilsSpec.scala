package org.deepdive.test.unit

import org.scalatest._
import org.deepdive.extraction.datastore._
import play.api.libs.json._

class FileDataUtilsSpec extends FunSpec {

  val sampleCSVFile = getClass.getResource("/sample.csv").getFile

  describe("Querying a file as JSON") {
    
    it("should work with absolute paths") {
      FileDataUtils.queryAsJson(sampleCSVFile, ',') { data =>
        assert(data.toList == List(
          JsArray(Seq(JsString("1"), JsString("2"), JsString("3"))),
          JsArray(Seq(JsString("Hello"), JsString("you"), JsString(""))),
          JsArray(Seq(JsString("A"), JsString("B"), JsString("C")))
        ))
      }
    }

  }

}