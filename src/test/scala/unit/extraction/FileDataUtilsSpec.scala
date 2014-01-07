package org.deepdive.test.unit

import org.scalatest._
import org.deepdive.extraction.datastore._
import spray.json._

class FileDataUtilsSpec extends FunSpec {

  val sampleCSVFile = getClass.getResource("/sample.csv").getFile

  describe("Querying a file as JSON") {
    it("should work") {
      FileDataUtils.queryAsJson(sampleCSVFile, ',') { data =>
        assert(data.toList == List(
          JsArray(JsString("1"), JsString("2"), JsString("3")),
          JsArray(JsString("Hello"), JsString("you"), JsString("")),
          JsArray(JsString("A"), JsString("B"), JsString("C"))
        ))
      }
    }
  }

}