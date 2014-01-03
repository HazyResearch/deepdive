package org.deepdive.test.integration

import anorm._ 
import com.typesafe.config._
import org.deepdive.test._
import org.deepdive.Context
import org.deepdive.Pipeline
import org.deepdive.datastore.{PostgresDataStore, JdbcDataStore}
import org.scalatest._
import scalikejdbc.ConnectionPool

class LogisticRegressionApp extends FunSpec {

  def prepareData() {
    JdbcDataStore.init(ConfigFactory.load)
    PostgresDataStore.withConnection { implicit conn =>
       SQL("drop schema if exists public cascade; create schema public;").execute()
       SQL("create table titles_tmp(id bigserial primary key, title text, has_extractions boolean);").execute()
       SQL("create table titles(id bigserial primary key, title text, has_extractions boolean);").execute()
       SQL("""create table word_presences(id bigserial primary key, 
        title_id bigint references titles(id), word text, is_present boolean);""").execute()
       SQL(
        """
          INSERT INTO titles_tmp(title, has_extractions) VALUES
          ('I am title 1', NULL), ('I am title 2', NULL), ('I am another Title', true)
        """).execute()
    }
    JdbcDataStore.close()
  }

  def getConfig = {
    s"""
      deepdive.schema.variables {
        word_presences.is_present: Boolean
        titles.has_extractions: Boolean
      }

      deepdive.extraction.extractors: {
        titlesExtractor.output_relation: "titles"
        titlesExtractor.input: "SELECT * from titles_tmp"
        titlesExtractor.udf: "/usr/bin/sed -e s/titles_tmp.//g"
        
        wordsExtractor.output_relation: "word_presences"
        wordsExtractor.input: "SELECT * FROM titles"
        wordsExtractor.udf: "${getClass.getResource("/logistic_regression/word_extractor.py").getFile}"
        wordsExtractor.dependencies = ["titlesExtractor"]
      }

      deepdive.inference.factors {
        wordFactor.input_query = "SELECT word_presences.*, titles.* FROM word_presences INNER JOIN titles ON word_presences.title_id = titles.id"
        wordFactor.function: "titles.has_extractions = Imply(word_presences.is_present)"
        wordFactor.weight: "?(word_presences.word)"
      }

    """
  }

  it("should work") {
    prepareData()
    val config = ConfigFactory.parseString(getConfig).withFallback(ConfigFactory.load)
    Pipeline.run(config)
    // Make sure the data is in the database
    JdbcDataStore.init(ConfigFactory.load)
    PostgresDataStore.withConnection { implicit conn =>
     
      val extractionResult = SQL("SELECT * FROM word_presences;")().map { row =>
       row[Long]("id")
      }.toList
      assert(extractionResult.size == 12)
      
      val numFactors = SQL("select count(*) as c from factors;")().head[Long]("c")
      val numVariables = SQL("select count(*) as c from variables;")().head[Long]("c")
      val numFactorVariables = SQL("select count(*) as c from factor_variables;")().head[Long]("c")
      val numWeights = SQL("select count(*) as c from weights;")().head[Long]("c")

      // One variable for each word, and one variable for each title
      assert(numVariables == 15)
      // One factor for each word
      assert(numFactors == 12)
      // Each factor connects one word and one title (12*2)
      assert(numFactorVariables == 24)
      // Different weight for each unique word
      assert(numWeights == 7)

      // Make sure the variables types are correct
      val numEvidence = SQL("""
        select count(*) as c from variables 
        WHERE is_evidence = true""")().head[Long]("c")
      val numQuery = SQL("""
        select count(*) as c from variables 
        WHERE is_query = true""")().head[Long]("c")
      // 1 title and 12 words are evidence
      assert(numEvidence == 13)
      assert(numQuery == 2)

    }
  }


}