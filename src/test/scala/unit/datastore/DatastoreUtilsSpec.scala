package org.deepdive.test.unit

import anorm._
import org.scalatest._
import play.api.libs.json._
import play.api.libs.json._
import org.deepdive.datastore.DataStoreUtils

class DatabaseUtilsSpec extends FunSpec {

  describe("Converting a JSON row to an Anorm Sequence") {

    it("should work") {
      val row = JsObject(Map[String, JsValue](
        "nullField" -> JsNull,
        "stringField" -> JsString("I am a string"),
        "longField" -> JsNumber(100),
        "booleanField" -> JsBoolean(true)
      ).toSeq)
      val result = DataStoreUtils.jsonRowToAnormSeq(row).toMap
      assert(result.mapValues(_.aValue) == Map(
        ("nullField", null),
        ("stringField", "I am a string"),
        ("longField", 100),
        ("booleanField", true)
      ))
    }
  }

}