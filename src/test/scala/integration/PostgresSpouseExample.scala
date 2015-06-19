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
import org.deepdive.datastore._
import org.scalatest._
import org.deepdive.Logging
import org.deepdive.settings._
import org.deepdive.helpers.Helpers
import org.deepdive.test.helpers.TestHelper
import java.io._
import scala.sys.process._
import scalikejdbc.ConnectionPool

/** Text running spouse example.
 *
 */
class PostgresSpouseExample extends FunSpec with Logging with PostgresDataStoreComponent {

  // Read the schema from test file
  val schema = scala.io.Source.fromFile(getClass.getResource("/spouse/schema_psql.sql").getFile).mkString
  val config = ConfigFactory.parseString(getConfig).withFallback(ConfigFactory.load).resolve()


    /** prepare data */
  def prepareData() {

    JdbcDataStoreObject.init(config)
    dataStore.withConnection { implicit conn =>

      JdbcDataStoreObject.executeSqlQueries(schema);

      Helpers.executeSqlQueriesByFile(TestHelper.getDbSettings,
              s"""\\COPY sentences FROM '${getClass.getResource(
              "/spouse/data/sentences_dump.csv").getFile}' CSV;""" )
    }
    JdbcDataStoreObject.close()
  }


  /** application.conf configuration */

  def getConfig = TestHelper.getConfig() +
  s"""
deepdive {

  # Put your variables here
  schema.variables {
    has_spouse.is_true: Boolean
  }

  # Put your extractors here
  extraction.extractors {

    # Clean sentence table
    ext_clear_sentence {
      style: "sql_extractor"
      sql: \"\"\"DELETE FROM sentences;\"\"\"
    }

    # Clean output tables of all extractors
    ext_clear_table {
      style: "sql_extractor"
      sql: \"\"\"
        DELETE FROM people_mentions;
        DELETE FROM has_spouse;
        DELETE FROM has_spouse_features;
        \"\"\"
    }

    # With a tsv_extractor, developers have to make sure arrays
      # are parsable in the UDF. One easy way is to
      # use "array_to_string(array, delimiter)" function by psql.
    ext_people {
      input: \"\"\"
          SELECT  sentence_id,
                  array_to_string(words, '~^~') AS words,
                  array_to_string(ner_tags, '~^~') AS ner_tags
          FROM    sentences
          \"\"\"
      output_relation: "people_mentions"
      udf: ${getClass.getResource("/spouse/udf/ext_people.py").getFile}
      dependencies: ["ext_sentences", "ext_clear_table"]
      input_batch_size: 4000
      style: "tsv_extractor"
    }

    ext_has_spouse_candidates {
      input: \"\"\"
       SELECT p1.sentence_id,
              p1.mention_id AS mention_id_1,
              p1.text AS text_1,
              p2.mention_id AS mention_id_2,
              p2.text AS text_2
        FROM  people_mentions p1,
              people_mentions p2
        WHERE p1.sentence_id = p2.sentence_id
          AND p1.mention_id != p2.mention_id;
          \"\"\"
      output_relation: "has_spouse"
      udf: ${getClass.getResource("/spouse/udf/ext_has_spouse.py").getFile}
      dependencies: ["ext_people"]
      style: "tsv_extractor"
    }

    ext_has_spouse_features {
      input: \"\"\"
        SELECT  array_to_string(words, '~^~') AS words,
                has_spouse.relation_id,
                p1.start_position AS start_position_1,
                p1.length AS length_1,
                p2.start_position AS start_position_2,
                p2.length AS length_2
        FROM    has_spouse,
                people_mentions p1,
                people_mentions p2,
                sentences
        WHERE   has_spouse.person1_id = p1.mention_id
          AND   has_spouse.person2_id = p2.mention_id
          AND   has_spouse.sentence_id = sentences.sentence_id;
        \"\"\"
      output_relation: "has_spouse_features"
      udf: ${getClass.getResource("/spouse/udf/ext_has_spouse_features.py").getFile}
      dependencies: ["ext_has_spouse_candidates"]
      style: "tsv_extractor"
    }

  }

  inference.factors: {

    # We require developers to select:
    #   - reserved "id" column,
    #   - variable column,
    #   - weight dependencies,
    # for variable tables.
    f_has_spouse_features {
      input_query: \"\"\"
        SELECT  has_spouse.id AS "has_spouse.id",
                has_spouse.is_true AS "has_spouse.is_true",
                feature
        FROM    has_spouse,
                has_spouse_features
        WHERE   has_spouse_features.relation_id = has_spouse.relation_id
        \"\"\"
      function: "IsTrue(has_spouse.is_true)"
      weight: "?(feature)"
    }

    f_has_spouse_symmetry {
      input_query: \"\"\"
        SELECT  r1.is_true AS "has_spouse.r1.is_true",
                r2.is_true AS "has_spouse.r2.is_true",
                r1.id AS "has_spouse.r1.id",
                r2.id AS "has_spouse.r2.id"
        FROM    has_spouse r1,
                has_spouse r2
        WHERE   r1.person1_id = r2.person2_id
          AND   r1.person2_id = r2.person1_id
          \"\"\"
      function: "Equal(has_spouse.r1.is_true, has_spouse.r2.is_true)"
      # weight: "10" # We are pretty sure about this rule
      weight: "?" # We are pretty sure about this rule
    }

  }

  pipeline.run: "nonlp"
  pipeline.pipelines.nonlp: [
    "ext_people",
    "ext_has_spouse_candidates",
    "ext_has_spouse_features",
    "f_has_spouse_features", "f_has_spouse_symmetry"
    ]

  # Specify a holdout fraction
  calibration.holdout_fraction: 0.25

}

  """
  /** Process DeepDive's results */
  def processResults(): Double = {
    JdbcDataStoreObject.init(config)
    var score = 0.0;

    // There is a chance that num_incorrect is 0 in bucket=9, in this case
    // num_incorrect will be NULL rather than 0 in the
    // has_spouse_is_true_calibration table. Not sure if there is a design
    // decision there or is this a bug. But this query will try to compute
    // X / (X + NULL) and get NULL as result, which breaks the result.
    val checkQuery = """select num_correct::real / (num_correct +
      CASE WHEN num_incorrect IS NULL THEN 0 ELSE num_incorrect END)
      from has_spouse_is_true_calibration where bucket = 9"""

    dataStore.withConnection { implicit conn =>
      JdbcDataStoreObject.executeSqlQueryWithCallback(checkQuery) { rs =>
        score = rs.getDouble(1)
      }
    }
    JdbcDataStoreObject.close()
    score
  }

  /**
   * Run the test
   */
  describe("Spouse example") {

    it("should get accuracy >0.9 for high-confidence predictions") {
      // Only test for Postgres for now
      assume(TestHelper.getTestEnv() == TestHelper.Psql)

      prepareData()
      Helpers.executeCmd("rm -f out/test_spouse/tmp/*")
      DeepDive.run(config, "out/test_spouse")
      // DeepDive.run will finally call JdbcDataStoreObject.close()...

      val score = processResults()
      System.err.println(s"score = ${score}")
      assert(score > 0.9)

      JdbcDataStoreObject.close()
    }
  }


}
