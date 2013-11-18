package org.deepdive.test.integration

import anorm._ 
import com.typesafe.config._
import org.deepdive.context._
import org.deepdive.Pipeline
import org.deepdive.datastore.PostgresDataStore
import org.scalatest._
import scalikejdbc.ConnectionPool

class SampleApp extends FunSpec {

  def prepareData() {
    PostgresDataStore.init("jdbc:postgresql://localhost/deepdive_test", "dennybritz", "")
    PostgresDataStore.withConnection { implicit conn =>
       SQL("drop schema if exists public cascade; create schema public;").execute()
       SQL("create table words (id bigserial primary key, sentence_id integer, position integer, text text);").execute()
       SQL("create table entities (id bigserial primary key, word_id integer, text text);").execute()
       SQL(
        """
          insert into words(sentence_id, position, text) VALUES
          (1, 0, 'Sam'), (1, 1, 'is'), (1, 2, 'very'), (1, 3, 'happy'),
          (2, 0, 'Alice'), (2, 1, 'loves'), (2, 2, 'Bob'), (2, 3, 'today')
        """).execute()
    }
  }

  def getConfig = {
    s"""
      deepdive.global.connection: {
        host: "localhost"
        port: 5432
        db: "deepdive_test"
        user: "dennybritz"
        password: ""
      }

      deepdive.relations.words.schema: { id: Integer, sentence_id: Integer, position: Integer, text: Text }
      deepdive.relations.words.fkeys : {}
      deepdive.relations.entities.schema: { id: Integer, word_id: Integer, text: Text }
      deepdive.relations.entities.fkeys : {}

      deepdive.ingest = {}

      deepdive.extractions: {
        entitiesExtractor.output_relation: "entities"
        entitiesExtractor.input: "SELECT * FROM words"
        entitiesExtractor.udf: "${getClass.getResource("/sample/sample_entities.py").getFile}"
        entitiesExtractor.factor.name: "Entities"
        entitiesExtractor.factor.function: "Imply()"
        entitiesExtractor.factor.weight: 5
      }
    """
  }

  it("should work") {
    prepareData()
    val config = ConfigFactory.parseString(getConfig)
    Pipeline.run(config)
    // Make sure the data is in the database
    PostgresDataStore.withConnection { implicit conn =>
      val result = SQL("SELECT * FROM entities;")().map { row =>
       row[String]("text")
      }.toList
      assert(result.size == 3)
      assert(result == List("Sam", "Alice", "Bob"))
    }
  }


}