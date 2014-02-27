package org.deepdive.test.unit

import anorm._
import java.io.{File, FileOutputStream}
import org.deepdive.calibration._
import org.deepdive.inference._
import org.deepdive.test._
import org.scalatest._
import org.deepdive.datastore._
import org.deepdive.settings._
import scala.io.Source

class PostgresInferenceDataStoreSpec extends FunSpec with BeforeAndAfter
  with PostgresInferenceDataStoreComponent {

  lazy implicit val connection = inferenceDataStore.connection

  /* Initialize and clear the data store before each test */
  before {
    JdbcDataStore.init()
    SQL("drop schema if exists public cascade; create schema public;").execute()
  }

  after {
    JdbcDataStore.close()
  }

  describe("Postgres inference data store") {
    
    describe("intializing") {
      it("should work") {
        inferenceDataStore.init()
      }
    }

    describe("grounding the factor graph") {
      it("should work")(pending)
    }

  

    describe("dumping the factor graph") {

      it("should work") {
        inferenceDataStore.init()

        // Insert weights
        SQL("""INSERT INTO dd_graph_weights(id, initial_value, is_fixed, description)
          VALUES (0, 0.0, false, 'w1'), (1, 0.0, false, 'w2')""").execute()
        // Insert variables
        SQL("""INSERT INTO dd_graph_variables(id, data_type, initial_value, is_evidence)
          VALUES (0, 'Boolean', 0.0, false), (1, 'Boolean', 1.0, true), 
          (2, 'Multinomial', 3.0, false)""").execute()
        SQL("""INSERT INTO dd_graph_local_variable_map(id, mrel, mcol, mid)
          VALUES (0, 'r1', 'c1', 0), (1, 'r1', 'c1', 1), 
          (2, 'r2', 'c2', 2)""").execute()
        // Insert factors
        SQL("""INSERT INTO dd_graph_factors(id, weight_id, factor_function)
          VALUES (0, 0, 'ImplyFactorFunction'), (1, 1, 'ImplyFactorFunction')""").execute()
        // Insert edges
        SQL("""INSERT INTO dd_graph_edges(factor_id, variable_id, position, is_positive)
          VALUES (0, 0, 0, true), (1, 1, 0, true), (1, 2, 1, true)""").execute()

        // Dump the factor graph
        val weightsFile = File.createTempFile("weights", "pb")
        val variablesFile = File.createTempFile("variables", "pb")
        val factorsFile = File.createTempFile("factors", "pb")
        val edgesFile = File.createTempFile("edges", "pb")
        val weightsOut = new FileOutputStream(weightsFile)
        val variablesOut = new FileOutputStream(variablesFile)
        val factorsOut = new FileOutputStream(factorsFile)
        val edgesOut = new FileOutputStream(edgesFile)
        val serializer = new ProtobufSerializer(weightsOut, variablesOut, factorsOut, edgesOut)

        inferenceDataStore.dumpFactorGraph(serializer, Map("r1.c1" -> BooleanType, "r2.c2" -> MultinomialType(5)))
        weightsOut.close()
        variablesOut.close()
        factorsOut.close()
        edgesOut.close()

        assert(weightsFile.exists() === true)
        assert(variablesFile.exists() === true)
        assert(factorsFile.exists() === true)
        assert(edgesFile.exists() === true)
      }

    }

    describe ("writing back the inference Result") {

      val variablesFile = getClass.getResource("/inference/sample_result.variables.pb").getFile
      val weightsFile = getClass.getResource("/inference/sample_result.weights.pb").getFile
      val schema = Map[String, VariableDataType]("has_spouse.is_true" -> BooleanType)

      it("should work") {
        inferenceDataStore.init()
        SQL("""create table has_spouse(id bigserial primary key, is_true boolean)""").execute()
        inferenceDataStore.writebackInferenceResult(schema, variablesFile, weightsFile)
      }

    }

    describe ("Getting the calibration data") {


      it("should work for Boolean variables") {
        SQL("""create table t1_c1_inference(id bigserial primary key, c1 boolean, 
          category bigint, expectation double precision)""").execute()
        SQL("""insert into t1_c1_inference(c1, category, expectation) VALUES
          (null, null, 0.31), (null, null, 0.93), (null, null, 0.97), 
          (false, null, 0.0), (true, null, 0.77), (true, null, 0.81)""").execute()
        val buckets = Bucket.ten
        val result = inferenceDataStore.getCalibrationData("t1.c1", BooleanType, buckets)
        assert(result == buckets.zip(List(
          BucketData(1, 0, 1),
          BucketData(0, 0, 0),
          BucketData(0, 0, 0),
          BucketData(1, 0, 0),
          BucketData(0, 0, 0),
          BucketData(0, 0, 0),
          BucketData(0, 0, 0),
          BucketData(1, 1, 0),
          BucketData(1, 1, 0),
          BucketData(2, 0, 0)
        )).toMap)
      }

      it("should work for categorical variables") {
         SQL("""create table t1_c1_inference(id bigserial primary key, c1 bigint, 
          category bigint, expectation double precision)""").execute()
         SQL("""insert into t1_c1_inference(c1, category, expectation) VALUES
          (null, 0, 0.55), (null, 1, 0.55), (null, 2, 0.55), 
          (0, 0, 0.65), (0, 1, 0.95), (0, 2, 0.95), 
          (1, 0, 0.85), (1, 1, 0.95), (1, 2, 0.95)""").execute()
        val buckets = Bucket.ten
        val result = inferenceDataStore.getCalibrationData("t1.c1", MultinomialType(3), buckets)
        assert(result(buckets(5)) == BucketData(3, 0, 0))
        assert(result(buckets(6)) == BucketData(1, 1, 0))
        assert(result(buckets(8)) == BucketData(1, 0, 1))
        assert(result(buckets(9)) == BucketData(4, 1, 3))
      }

    }
    
  }

}
