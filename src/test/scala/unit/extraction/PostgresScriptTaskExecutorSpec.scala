package org.deepdive.test.unit

import anorm._
import org.deepdive.datastore.PostgresDataStore
import org.deepdive.extraction._
import org.deepdive.extraction.datastore._
import org.deepdive.test._
import org.deepdive.settings.Extractor
import org.scalatest._
import spray.json._
import DefaultJsonProtocol._

class ScriptTaskExecutorSpec extends FunSpec with BeforeAndAfter 
  with PostgresExtractionDataStoreComponent {

  lazy implicit val connection = dataStore.connection

  before {
    PostgresTestDataStore.init()
    SQL("drop schema if exists public cascade; create schema public;").execute()
    SQL("create table documents(id bigserial primary key, docid integer, text text);").execute()
    SQL("insert into documents(docid, text) VALUES (469, 'Document 1'), (470, 'Document 2')").execute()
  }

  describe("running") {    

    it("should work with a simple query") {
      val extractorFile = getClass.getResource("/simple_extractor.py")
      val task = ExtractionTask(Extractor("test", "output", "SELECT * FROM documents", extractorFile.getFile, Set()))
      val executor = new ScriptTaskExecutor(task, this)
      val result = executor.run()
      assert(result.rows.map(_.compactPrint) == List(Map("document_id" -> 469), Map
        ("document_id" -> 470)).map(_.toJson.compactPrint))
    }
  }


}