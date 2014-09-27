/******************************
 * 
 * DeepDive.run cannot be called more than once in integration
 * tests, so we run tests one by one to cover them.
 * 
 ******************************/
package org.deepdive.test.integration

import anorm._ 
import com.typesafe.config._
import org.deepdive.test._
import org.deepdive.Context
import org.deepdive._
import org.deepdive.datastore.{PostgresDataStore, JdbcDataStore}
import org.scalatest._
import org.deepdive.Logging
import java.io._
import scala.sys.process._
import scalikejdbc.ConnectionPool

/** Text chunking with linear chain CRF. Test whether we get a reasonable F1 score.
 * 
 * Please refer to examples/chunking for more details.
 */
class ChunkingApp extends FunSpec {

  /** prepare data */
  def prepareData() {
    JdbcDataStore.init(ConfigFactory.load)
    PostgresDataStore.withConnection { implicit conn =>
      SQL("drop schema if exists public cascade; create schema public;").execute()
      SQL("""create table words_raw(
        word_id bigserial,
        word text,
        pos text,
        tag text,
        id bigint);""").execute()
      SQL("""create table words(
        sent_id bigint,
        word_id bigint,
        word text,
        pos text,
        true_tag text,
        tag int,
        id bigint);""").execute()
      SQL("""create table word_features(
        word_id bigint,
        feature text,
        id bigint);""").execute()
      SQL(s"""copy words_raw(word, pos, tag) from '${getClass.getResource("/chunking/data/train_null_terminated.txt").getFile}' 
        delimiter ' ' null 'null';""").execute()
      SQL(s"""copy words_raw(word, pos, tag) from '${getClass.getResource("/chunking/data/test_null_terminated.txt").getFile}' 
        delimiter ' ' null 'null';""").execute()
    }
    JdbcDataStore.close()
  }

  def query1 = s"""${"\"\"\""}
    select w1.word_id as "w1.word_id", w1.word as "w1.word", w1.pos as "w1.pos", 
      w2.word as "w2.word", w2.pos as "w2.pos"
    from words w1, words w2
    where w1.word_id = w2.word_id + 1 and w1.word is not null ${"\"\"\""}"""

  def query2 = s"""${"\"\"\""}
    select words.id as "words.id", words.tag as "words.tag", word_features.feature as "feature" 
    from words, word_features 
    where words.word_id = word_features.word_id and words.word is not null ${"\"\"\""}"""

  def query3 = s"""${"\"\"\""}
    select w1.id as "words.w1.id", w2.id as "words.w2.id", w1.tag as "words.w1.tag", w2.tag as "words.w2.tag"
    from words w1, words w2
    where w2.word_id = w1.word_id + 1 ${"\"\"\""}"""

  /** application.conf configuration */
  def getConfig = s"""

    deepdive {

      schema.variables {
        words.tag: Categorical(13)
      }

      extraction.extractors {
        # extract training data
        ext_training {
          input: "select * from words_raw"
          output_relation: "words"
          udf: "${getClass.getResource("/chunking/udf/ext_training.py").getFile}"
        }

        # add features
        ext_features.input: ${query1}
        ext_features.output_relation: "word_features"
        ext_features.udf: "${getClass.getResource("/chunking/udf/ext_features.py").getFile}"
        ext_features.dependencies: ["ext_training"]

      }

      inference.factors {

        factor_feature {
          input_query: ${query2}
          function: "Multinomial(words.tag)"
          weight: "?(feature)"
        }

        factor_linear_chain_crf {
          input_query: ${query3}
          function: "Multinomial(words.w1.tag, words.w2.tag)"
          weight: "?"
        }

      } 

      calibration: {
        holdout_query: "INSERT INTO dd_graph_variables_holdout(variable_id) SELECT id FROM words WHERE word_id > 50078"
      }
    }
    """

  /** Process DeepDive's results */
  def processResults() : Double = {
    JdbcDataStore.init(ConfigFactory.load)
    val resultFile = File.createTempFile("result", "")
    resultFile.setWritable(true, false);
    
    PostgresDataStore.withConnection { implicit conn =>
      SQL("""drop table if exists result cascade;""").execute()
      SQL("""create table result
        (word_id bigint, word text, pos text, true_tag text, tag text);""").execute()
      SQL("""insert into result 
        select b.word_id, b.word, b.pos, b.true_tag, b.category
        from (select word_id, max(expectation) as m 
          from words_tag_inference group by word_id 
        ) as a inner join words_tag_inference as b
        on a.word_id = b.word_id and a.m = b.expectation;""").execute()
      SQL("""copy (select word, pos, true_tag, max(tag) from result 
        group by word_id, word, pos, true_tag order by word_id) to""" + 
        s""" '${resultFile.getAbsolutePath()}' delimiter ' ';""").execute()
    }
    JdbcDataStore.close()
    val converter = s"""${getClass.getResource("/chunking/convert.py").getFile}"""
    val evaluator = s"""${getClass.getResource("/chunking/conlleval.pl").getFile}"""
    val cmd = s"python ${converter} ${resultFile.getAbsolutePath()}" #| s"perl ${evaluator}"
    val f1 = cmd.!!
    f1.toDouble
  }
  describe("Chunking with linear chain CRF") {
    it("should get F1 score > 0.8") {
      prepareData()
      val config = ConfigFactory.parseString(getConfig).withFallback(ConfigFactory.load)
      DeepDive.run(config, "out/test_chunking")
      // Make sure the data is in the database
      val f1 = processResults()
      assert(f1 > 80)
    }
  }


}
