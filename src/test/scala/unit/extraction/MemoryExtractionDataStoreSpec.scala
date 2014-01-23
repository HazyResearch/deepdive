package org.deepdive.test.unit

import org.deepdive.extraction.datastore._
import org.scalatest._
import play.api.libs.json._

class MemoryExtractionDataStoreSpec extends FunSpec with BeforeAndAfter
  with MemoryExtractionDataStoreComponent {


  describe("Writing results") {
    
    it("should work") {
      val testRow = JsObject(Map[String, JsValue](
        "id" -> JsNumber(1),
        "key" -> JsNumber(100),
        "some_text" -> JsString("I am sample text."),
        "some_boolean" -> JsBoolean(false),
        "some_double" -> JsNumber(13.37)
      ).toSeq)
      dataStore.addBatch(List(testRow).iterator, "testRelation")
      assert(dataStore.data.size == 1)
    }

  }

  describe("Reading as JSON") {
    
    it("should work") {
      val testRow = JsObject(Map[String, JsValue](
        "id" -> JsNumber(1),
        "key" -> JsNumber(100),
        "some_text" -> JsString("I am sample text."),
        "some_boolean" -> JsBoolean(false),
        "some_double" -> JsNumber(13.37)
      ).toSeq)
      dataStore.addBatch(List(testRow).iterator, "testRelation")
      val queryResult = dataStore.queryAsJson("testRelation") { data =>
        assert(data.toList.head == testRow)
      }
    }

    it("should work with empty relations") {
      val queryResult = dataStore.queryAsJson("doesNotExist") { data =>
        assert(data.toList === Nil)
      }
    }

  }

  describe("Reading as Map") {

    it("should work"){
      val testRow = JsObject(Map[String, JsValue](
        "id" -> JsNumber(1),
        "key" -> JsNumber(100),
        "some_text" -> JsString("I am sample text."),
        "some_boolean" -> JsBoolean(false),
        "some_double" -> JsNumber(13.37)
      ).toSeq)
      dataStore.addBatch(List(testRow).iterator, "testRelation")
      val queryResult = dataStore.queryAsMap("testRelation")(_.toList)
      assert(queryResult.head == Map[String, Any](
        "id" -> BigDecimal(1),
        "key" -> BigDecimal(100),
        "some_text" -> "I am sample text.",
        "some_boolean" -> false,
        "some_double" -> BigDecimal(13.37)
      ))
    }

  }

}