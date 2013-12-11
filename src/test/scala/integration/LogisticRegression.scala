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
       SQL("create table titles(id bigserial primary key, title text, has_extractions boolean);").execute()
       SQL("""create table word_presences(id bigserial primary key, 
        title_id bigint references titles(id), word text, is_present boolean);""").execute()
       SQL(
        """
          insert into titles(title, has_extractions) VALUES
          ('I am title 1', NULL), ('I am title 2', NULL), ('I am another Title', true)
        """).execute()
    }
  }1

  def getConfig = {
    s"""
      deepdive.global.connection: {
        host: "localhost"
        port: 5432
        db: "deepdive_test"
        user: "dennybritz"
        password: ""
      }

      deepdive.relations.words.schema: { id: Integer, sentence_id: Integer, position: Integer, text: Text, is_word: Boolean }
      deepdive.relations.words.query_field : "is_word"
      deepdive.relations.entities.schema: { id: Integer, word_id: Integer, text: Text, is_true: Boolean}
      deepdive.relations.entities.fkeys : { word_id: "words.id" }
      deepdive.relations.entities.query_field : "is_true"

      deepdive.extractions: {
        entitiesExtractor.output_relation: "entities"
        entitiesExtractor.input: "SELECT * FROM words"
        entitiesExtractor.udf: "${getClass.getResource("/sample/sample_entities.py").getFile}"
      }

      deepdive.factors {
        entities.relation = "entities"
        entities.function: "is_true = Imply(word_id->is_word)"
        entities.weight: "?(text)"
      }
    """
  }

  it("should work") {
    prepareData()
    val config = ConfigFactory.parseString(getConfig)
    Pipeline.run(config)
    // Make sure the data is in the database
    PostgresDataStore.withConnection { implicit conn =>
     
      val extractionResult = SQL("SELECT * FROM entities;")().map { row =>
       row[String]("text")
      }.toList
      assert(extractionResult.size == 4)
      assert(extractionResult == List("Lara", "Sam", "Alice", "Sam"))

      val numFactors = SQL("select count(*) as c from factors;")().head[Long]("c")
      val numVariables = SQL("select count(*) as c from variables;")().head[Long]("c")
      val numFactorVariables = SQL("select count(*) as c from factor_variables;")().head[Long]("c")
      val numWeights = SQL("select count(*) as c from weights;")().head[Long]("c")

      // One factor for each variable in entities
      assert(numFactors == 4)
      assert(numVariables == 12)
      // Entitiy factors have two variables
      assert(numFactorVariables == 8)
      // 3 different weights for entities
      assert(numWeights == 3)

      // Make sure the variables types are correct
      val numEvidence = SQL("""
        select count(*) as c from variables 
        WHERE is_evidence = true""")().head[Long]("c")
      val numQuery = SQL("""
        select count(*) as c from variables 
        WHERE is_query = true""")().head[Long]("c")
      assert(numEvidence == 9)
      assert(numQuery == 3)

    }
  }


}