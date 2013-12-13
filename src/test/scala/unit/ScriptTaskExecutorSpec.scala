package org.deepdive.test.unit

import anorm._
import org.deepdive.datastore.PostgresDataStore
import org.deepdive.extraction._
import org.deepdive.test._
import org.scalatest._
import scala.io.Source
import spray.json._
import DefaultJsonProtocol._

class ScriptTaskExecutorSpec extends FunSpec {


  describe("Serializing to JSON") {

    def prepareData() {
      TestDataStore.init()
      PostgresDataStore.withConnection { implicit conn =>
       SQL("drop schema if exists public cascade; create schema public;").execute()
       SQL("""create table datatype_test(id bigserial primary key, key integer, some_text text, 
        some_boolean boolean, some_double double precision);""").execute()
       SQL(
        """
          insert into datatype_test(key, some_text, some_boolean, some_double) 
          VALUES (1, 'Hello', true, 1.0), (1, 'Ce', false, 2.3)
        """).execute()
     }
    }

    it("should work with aggregate data types") {
      prepareData()
      implicit val conn = PostgresDataStore.borrowConnection()
      val result = SQL("""
        SELECT key, array_agg(some_text) AS "datatype_test.texts"
        FROM datatype_test GROUP BY key"""
      )().map { row =>
        ScriptTaskExecutor.sqlRowToJson(row)
      }.toList
      assert(result.head == Map[String, JsValue](
        "datatype_test.key" -> JsNumber(1),
        ".datatype_test.texts" -> JsArray(JsString("Hello"), JsString("Ce"))
      ))
    }

    it("should work with simple data types") {
      prepareData()
      implicit val conn = PostgresDataStore.borrowConnection()
      val result = SQL("SELECT * from datatype_test")().map { row =>
        ScriptTaskExecutor.sqlRowToJson(row)
      }.toList
      assert(result.head == Map[String, JsValue](
        "datatype_test.id" -> JsNumber(1),
        "datatype_test.key" -> JsNumber(1),
        "datatype_test.some_text" -> JsString("Hello"),
        "datatype_test.some_boolean" -> JsBoolean(true),
        "datatype_test.some_double" -> JsNumber(1.0)
      ))
    }

  }

  describe("running") {
    
    def prepareData() {
      TestDataStore.init()
      PostgresDataStore.withConnection { implicit conn =>
         SQL("drop schema if exists public cascade; create schema public;").execute()
         SQL("create table documents(id bigserial primary key, docid integer, text text);").execute()
         SQL(
          """
            insert into documents(docid, text) 
            VALUES (469, 'Document 1'), (470, 'Document 2')
          """).execute()
      }
    }

    it("should work with a basic query") {
      prepareData()
      PostgresDataStore.init("jdbc:postgresql://localhost/deepdive_test", "dennybritz", "")
      val extractorFile = getClass.getResource("/simple_extractor.py")
      val task = ExtractionTask("test", "output", "SELECT * FROM documents", extractorFile.getFile)
      val executor = new ScriptTaskExecutor(task)
      val result = executor.run()
      assert(result.rows.map(_.compactPrint) == List(Map("document_id" -> 469), Map
        ("document_id" -> 470)).map(_.toJson.compactPrint))
    }
  }


}