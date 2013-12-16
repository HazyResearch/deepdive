package org.deepdive.test.unit

import anorm._
import org.deepdive.datastore.PostgresDataStore
import org.deepdive.extraction._
import org.deepdive.extraction.datastore._
import org.deepdive.test._
import org.scalatest._
import scala.io.Source
import spray.json._
import DefaultJsonProtocol._

class PostgresExtractionDataStoreSpec extends FunSpec with BeforeAndAfter
  with PostgresExtractionDataStoreComponent {

  lazy implicit val connection = dataStore.connection

  before {
    PostgresTestDataStore.init()
    SQL("drop schema if exists public cascade; create schema public;").execute()
    SQL("""create table datatype_test(id bigserial primary key, key integer, some_text text, 
      some_boolean boolean, some_double double precision);""").execute()
  }

  describe("Serializing to JSON") {  

    def insertSampleRow() : Unit = {
      SQL("""insert into datatype_test(key, some_text, some_boolean, some_double) 
        VALUES (1, 'Hello', true, 1.0), (1, 'Ce', false, 2.3)""").execute()
    }

    it("should work with aggregate data types") {
      insertSampleRow()
      val result = dataStore.queryAsJson(
        """SELECT key, array_agg(some_text) AS "datatype_test.texts"
        FROM datatype_test GROUP BY key"""
      ).toList
      assert(result.head.fields == Map[String, JsValue](
        "datatype_test.key" -> JsNumber(1),
        ".datatype_test.texts" -> JsArray(JsString("Hello"), JsString("Ce"))
      ))
    }

    it("should work with simple data types") {
      insertSampleRow()
      val result = dataStore.queryAsJson("SELECT * from datatype_test").toList
      assert(result.head.fields == Map[String, JsValue](
        "datatype_test.id" -> JsNumber(1),
        "datatype_test.key" -> JsNumber(1),
        "datatype_test.some_text" -> JsString("Hello"),
        "datatype_test.some_boolean" -> JsBoolean(true),
        "datatype_test.some_double" -> JsNumber(1.0)
      ))
    }
  }

  describe ("Writing JSON to the data store") {

    it("should work") {
      val testRow = JsObject(Map[String, JsValue](
        "id" -> JsNumber(1),
        "key" -> JsNumber(100),
        "some_text" -> JsString("I am sample text."),
        "some_boolean" -> JsBoolean(false),
        "some_double" -> JsNumber(13.37)
      ))
      dataStore.writeResult(List(testRow), "datatype_test")
      val rowCount = SQL("select count(*) as c from datatype_test")().head[Long]("c")
      assert(rowCount == 1)
    }

  }
}
  


    