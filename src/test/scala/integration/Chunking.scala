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
import org.deepdive.settings._
import org.deepdive.datastore._
import org.scalatest._
import org.deepdive.Logging
import org.deepdive.helpers.Helpers
import org.deepdive.test.helpers.TestHelper
import java.io._
import scala.sys.process._
import scalikejdbc.ConnectionPool

/** Text chunking with linear chain CRF. Test whether we get a reasonable F1 score.
 * 
 * Please refer to examples/chunking for more details.
 */
class ChunkingApp extends FunSpec with Logging{

  val config = ConfigFactory.parseString(getConfig).withFallback(ConfigFactory.load).resolve()
  val env = TestHelper.getTestEnv()

  val ds = env match {
    case TestHelper.Psql => PostgresDataStore
    case TestHelper.Mysql => MysqlDataStore
  }

  /** prepare data */
  def prepareData() {
    Helpers.executeCmd("rm -f out/test_chunking/tmp/*")
    JdbcDataStore.init(config)
    env match {
      case TestHelper.Psql =>
        ds.withConnection { implicit conn =>
          ds.executeSqlQueries("drop schema if exists public cascade; create schema public;")
          ds.executeSqlQueries("""create table words_raw(
            word_id bigserial,
            word text,
            pos text,
            tag text,
            id bigint);""")
          ds.executeSqlQueries("""create table words(
            sent_id bigint,
            word_id bigint,
            word text,
            pos text,
            true_tag text,
            tag int,
            id bigint);""")
          ds.executeSqlQueries("""create table word_features(
            word_id bigint,
            feature text,
            id bigint);""")
          ds.executeSqlQueries(s"""copy words_raw(word, pos, tag) from '${getClass.getResource("/chunking/data/train_null_terminated.txt").getFile}' 
            delimiter ' ';""")
          ds.executeSqlQueries(s"""copy words_raw(word, pos, tag) from '${getClass.getResource("/chunking/data/test_null_terminated.txt").getFile}' 
            delimiter ' ';""")
        }

      case TestHelper.Mysql =>
        ds.withConnection { implicit conn =>
          ds.executeSqlQueries("""create table words_raw(
            word_id bigint primary key auto_increment,
            word text,
            pos text,
            tag text,
            id bigint);""")
          ds.executeSqlQueries("""create table words(
            sent_id bigint,
            word_id bigint,
            word text,
            pos text,
            true_tag text,
            tag int,
            id bigint);""")
          ds.executeSqlQueries("""create table word_features(
            word_id bigint,
            feature text,
            id bigint);""")
          ds.executeSqlQueries(s"""LOAD DATA INFILE '${getClass.getResource("/chunking/data/train_null_terminated.txt").getFile}' 
            INTO TABLE words_raw FIELDS TERMINATED BY ' ' (word, pos, tag);""")
          ds.executeSqlQueries(s"""LOAD DATA INFILE '${getClass.getResource("/chunking/data/test_null_terminated.txt").getFile}' 
            INTO TABLE words_raw FIELDS TERMINATED BY ' ' (word, pos, tag);""")
        }

      case _ => 
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
      db.default {
        driver: ${TestHelper.getDriverFromEnv()}
        url: "${System.getenv("DBCONNSTRING")}"
        user: "${System.getenv("DBUSER")}"
        password: "${System.getenv("DBPASSWORD")}"
        dbname: "${System.getenv("DBNAME")}"
        host: "${System.getenv("DBHOST")}"
        port: "${System.getenv("DBPORT")}"
      }

      schema.variables {
        words.tag: Categorical(13)
      }

      extraction.extractors {
        # extract training data
        ext_training {
          style: "tsv_extractor"
          input: "select * from words_raw"
          output_relation: "words"
          udf: "${getClass.getResource("/chunking/udf/ext_training.py").getFile}"
        }

        # create index
        ext_index {
          dependencies: ["ext_training"]
          style: "sql_extractor"
          sql: "create index words_word_id_idx on words(word_id);"
        }

        # add features
        ext_features.style: "tsv_extractor"
        ext_features.input: ${query1}
        ext_features.output_relation: "word_features"
        ext_features.udf: "${getClass.getResource("/chunking/udf/ext_features.py").getFile}"
        ext_features.dependencies: ["ext_index"]

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

      inference.parallel_grounding: ${System.getenv("PARALLEL_GROUNDING") match {
        case "true" | "1" | "True" | "TRUE" => "true"
        case _ => "false"
      }}
    }
    """

  /** Process DeepDive's results */
  def processResults() : Double = {
    JdbcDataStore.init(config)
    val resultFile = File.createTempFile("result", "")
    resultFile.setWritable(true, false)

    ds.withConnection { implicit conn =>
      ds.executeSqlQueries("""drop table if exists result cascade;""")
      ds.executeSqlQueries("""create table result
        (word_id bigint, word text, pos text, true_tag text, tag text);""")
      ds.executeSqlQueries("""insert into result 
        select b.word_id, b.word, b.pos, b.true_tag, b.category
        from (select word_id, max(expectation) as m 
          from words_tag_inference group by word_id 
        ) as a inner join words_tag_inference as b
        on a.word_id = b.word_id and a.m = b.expectation;""")

      // Use DataLoader to unload result data into file
      val dbSettings = Settings.loadFromConfig(config).dbSettings
      val du = new DataLoader
      du.unload(resultFile.getName, resultFile.getAbsolutePath,
          dbSettings,  //  
          false,       // usingGreenPlum 
          """select word, pos, true_tag, max(tag) from result 
         group by word_id, word, pos, true_tag order by word_id""")
    } 


    JdbcDataStore.close()
    val converter = s"""${getClass.getResource("/chunking/convert.py").getFile}"""
    val evaluator = s"""${getClass.getResource("/chunking/conlleval.pl").getFile}"""
    // TODO: this sometimes stuck forever. It's a bug not reproducible.
    val cmd = s"python ${converter} ${resultFile.getAbsolutePath()}" #| s"perl ${evaluator}"
    log.debug(s"Executing evaluation command: ${cmd}")
    val f1 = cmd.!!
    resultFile.delete()
    f1.toDouble
  }

  describe("Chunking with linear chain CRF") {

    it("should get F1 score > 0.8") {
      // Assume GP is not running on the system, or skip this test
      assume("which gpfdist".! != 0)
      prepareData()
      DeepDive.run(config, "out/test_chunking")
      // Make sure the data is in the database
      val f1 = processResults()
      assert(f1 > 80)
    }
  }

}
