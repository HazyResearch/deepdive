package org.deepdive.test.unit

import java.io.{File, FileOutputStream}
import org.deepdive.calibration._
import org.deepdive.inference._
import org.deepdive.test._
import org.scalatest._
import org.deepdive.datastore._
import org.deepdive.settings._
import org.deepdive.Context
import scala.io.Source
import scalikejdbc._

trait SQLInferenceDataStoreSpec extends FunSpec with BeforeAndAfter { this: SQLInferenceDataStoreComponent =>

  def dataStoreHelper : JdbcDataStore

  lazy implicit val session = dataStoreHelper.DB.autoCommitSession()

  val dbSettings = DbSettings(null, null, null, null, null, null, null, null, null, null)

  def init() : Unit = {
    JdbcDataStore.init()
    SQL("drop schema if exists public cascade; create schema public;").execute.apply()
  }

  /* Initialize and clear the data store before each test */
  before {
    init()
    Context.outputDir = new File(".").getCanonicalPath() + "/out"
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

    describe("grounding the factor graph with Boolean variables") {
      

      it("should work with a one-variable factor rule") {
        inferenceDataStore.init()
        
        // Insert sample data
        SQL(s"""CREATE TABLE r1(weight ${inferenceDataStore.stringType},
          is_correct boolean, id bigint);""").execute.apply()        
        val data = (1 to 100).map { i =>
          Map("id" -> i, "weight" -> s"weight_${i}", "is_correct" -> s"${i%2==0}".toBoolean)
        }
        dataStoreHelper.bulkInsert("r1", data.iterator)

        val schema = Map[String, VariableDataType]("r1.is_correct" -> BooleanType)

        // Build the factor description
        val factorDesc = FactorDesc("testFactor", 
            """SELECT id AS "r1.id", weight AS "weight", is_correct AS "r1.is_correct" FROM r1""", 
          IsTrueFactorFunction(Seq("r1.is_correct")), 
          UnknownFactorWeight(List("weight")), "weight_prefix")
        val holdoutFraction = 0.0

        // Ground the graph
        inferenceDataStore.groundFactorGraph(schema, Seq(factorDesc), holdoutFraction, None, false, "", dbSettings, false)

        // Check the result
        val numWeights = SQL(s"""SELECT COUNT(*) AS "count" FROM ${inferenceDataStore.WeightsTable}""")
          .map(rs => rs.long("count")).single.apply().get
        assert(numWeights === 100)
        val factorName = SQL(s"""SELECT * FROM ${inferenceDataStore.FactorMetaTable}""")
          .map(rs => rs.string("name")).single.apply().get
        assert(factorName === "testFactor")
        val funcid = SQL(s"""SELECT * FROM ${inferenceDataStore.FactorMetaTable}""")
          .map(rs => rs.int("funcid")).single.apply().get
        assert(funcid === 4)
        val signs = SQL(s"""SELECT * FROM ${inferenceDataStore.FactorMetaTable}""")
          .map(rs => rs.string("sign")).single.apply().get
        assert(signs === "true")
        val numFactors = SQL(s"""SELECT COUNT(*) AS "count" FROM dd_query_testFactor""")
          .map(rs => rs.long("count")).single.apply().get
        assert(numFactors === 100)
      }

      it("should work with custom a holdout query") {
        inferenceDataStore.init()
        
        // Insert sample data
        SQL(s"""CREATE TABLE r1(weight ${inferenceDataStore.stringType},
          is_correct boolean, id bigint);""").execute.apply()        
        val data = (1 to 100).map { i =>
          Map("id" -> i, "weight" -> s"weight_${i}", "is_correct" -> s"${i%2==0}".toBoolean)
        }
        dataStoreHelper.bulkInsert("r1", data.iterator)

        val schema = Map[String, VariableDataType]("r1.is_correct" -> BooleanType)

        // Build the factor description
        val factorDesc = FactorDesc("testFactor", 
            """SELECT id AS "r1.id", weight AS "weight", is_correct AS "r1.is_correct" FROM r1""", 
          IsTrueFactorFunction(Seq("r1.is_correct")), 
          UnknownFactorWeight(List("weight")), "weight_prefix")
        val holdoutFraction = 0.0

        // Ground the graph with custom holdout
        val customHoldoutQuery = """
          INSERT INTO dd_graph_variables_holdout(variable_id)
          SELECT id FROM r1 WHERE id <= 10;"""
        inferenceDataStore.groundFactorGraph(schema, Seq(factorDesc), holdoutFraction, Option(customHoldoutQuery), false, "", dbSettings, false)


        val numHoldout = SQL(s"""SELECT COUNT(*) AS "count" FROM r1
          WHERE id IN (SELECT variable_id FROM ${inferenceDataStore.VariablesHoldoutTable}) 
          AND is_correct = true;""")
          .map(rs => rs.long("count")).single.apply().get
        assert(numHoldout === 5)

      }

      it("should work with weight variables that are null") {
        inferenceDataStore.init()
         SQL(s"""CREATE TABLE r1(weight ${inferenceDataStore.stringType},
          is_correct boolean, id bigint);""").execute.apply()        
        val data = (1 to 100).map { i =>
          Map("id" -> i, "is_correct" -> s"${i%2==0}".toBoolean)
        }
        dataStoreHelper.bulkInsert("r1", data.iterator)

        val schema = Map[String, VariableDataType]("r1.is_correct" -> BooleanType)

        // Build the factor description
        val factorDesc = FactorDesc("testFactor", 
            """SELECT id AS "r1.id", weight AS "weight", is_correct AS "r1.is_correct" FROM r1""", 
          IsTrueFactorFunction(Seq("r1.is_correct")), 
          UnknownFactorWeight(List("weight")), "weight_prefix")
        val holdoutFraction = 0.0

        // Ground the graph
        inferenceDataStore.groundFactorGraph(schema, Seq(factorDesc), holdoutFraction, None, false, "", dbSettings, false)

        // Check the result
        val numWeights = SQL(s"""SELECT COUNT(*) AS "count" FROM ${inferenceDataStore.WeightsTable}""")
          .map(rs => rs.long("count")).single.apply().get
        assert(numWeights === 1)
        val weightdesc = SQL(s"""SELECT weight FROM dd_weights_testFactor""")
          .map(rs => rs.string("weight")).single.apply()
        assert(weightdesc === None)

      }

      it("should work with multi-variable factor rules") {
        inferenceDataStore.init()
        SQL(s"""CREATE TABLE r1(weight ${inferenceDataStore.stringType},
          is_correct boolean, id bigint);""").execute.apply()
        SQL(s"""CREATE TABLE r2(weight ${inferenceDataStore.stringType},
          is_correct boolean, id bigint);""").execute.apply()
        val data1 = (1 to 100).map { i =>
          Map("id" -> i, "weight" -> s"weight_${i}", "is_correct" -> s"${i%2==0}".toBoolean)
        }
        val data2 = (101 to 200).map { i =>
          Map("id" -> i, "weight" -> s"weight_${i}", "is_correct" -> s"${i%3==0}".toBoolean)
        }

        dataStoreHelper.bulkInsert("r1", data1.iterator)
        dataStoreHelper.bulkInsert("r2", data2.iterator)

        val schema = Map[String, VariableDataType]("r1.is_correct" -> BooleanType, "r2.is_correct" -> BooleanType)
        val factorDesc = FactorDesc("testFactor", 
          """SELECT r1.id AS "r1.id", r1.weight AS "weight", r1.is_correct AS "r1.is_correct",
          r2.id AS "r2.id", r2.is_correct AS "r2.is_correct" FROM r1, r2
          WHERE r1.id = (r2.id-100)""",
          AndFactorFunction(Seq("r1.is_correct", "r2.is_correct")), 
          UnknownFactorWeight(List("weight")), "weight_prefix")

        inferenceDataStore.groundFactorGraph(schema, Seq(factorDesc), 0.0, None, false, "", dbSettings, false)

        val numWeights = SQL(s"""SELECT COUNT(*) AS "count" FROM ${inferenceDataStore.WeightsTable}""")
          .map(rs => rs.long("count")).single.apply().get
        assert(numWeights === 100)
        val numFactors = SQL(s"""SELECT COUNT(*) AS "count" FROM dd_query_testFactor""")
          .map(rs => rs.long("count")).single.apply().get
        assert(numFactors === 100)


      }

    }

    describe("grounding the factor graph with Multinomial variables") {

      it("should work")(pending) 
      // {
    //     inferenceDataStore.init()
        
    //     // Create table with multinomial data
    //     SQL(s"""CREATE TABLE r1(id ${inferenceDataStore.keyType}, weight ${inferenceDataStore.stringType},
    //       value bigint);""").execute.apply() 
    //     val data = (1 to 100).map { i =>
    //       Map("id" -> i, "weight" -> s"weight_${i}", "value" -> (i%4))
    //     }
    //     dataStoreHelper.bulkInsert("r1", data.iterator)

    //     // Define the schema
    //     val schema = Map[String, VariableDataType]("r1.value" -> MultinomialType(4))

    //     // Build the factor description
    //     val factorDesc = FactorDesc("testFactor", 
    //       """SELECT id AS "r1.id", weight AS "weight", value AS "r1.value" FROM r1""", 
    //       IsTrueFactorFunction(Seq("r1.value")), 
    //       UnknownFactorWeight(List("weight")), "weight_prefix")
    //     val holdoutFraction = 0.0

    //     // Ground the graph
    //     inferenceDataStore.groundFactorGraph(schema, Seq(factorDesc), holdoutFraction, None, false, "", dbSettings)

    //     val numWeights = SQL(s"""SELECT COUNT(*) AS "count" FROM ${inferenceDataStore.WeightsTable}""")
    //       .map(rs => rs.long("count")).single.apply().get
    //     assert(numWeights === 400)
        
        // val numVariables = SQL(s"""SELECT COUNT(*) AS "count" FROM ${inferenceDataStore.VariablesTable}
        //   WHERE data_type = 'Multinomial' AND cardinality=4""")
        //   .map(rs => rs.long("count")).single.apply().get
        // assert(numVariables === 100)

        // One factor for each possible predicate assignment
        // val numFactors = SQL(s"""SELECT COUNT(*) AS "count" FROM ${inferenceDataStore.FactorsTable}""")
        //   .map(rs => rs.long("count")).single.apply().get
        // assert(numFactors === 400)

        // One edge for each possible predicate assignment
        // val numEdges = SQL(s"""SELECT COUNT(*) AS "count" FROM ${inferenceDataStore.EdgesTable}""")
        //   .map(rs => rs.long("count")).single.apply().get
        // assert(numEdges === 400)
        // val numEdgesPred0 = SQL(s"""SELECT COUNT(*) AS "count" FROM ${inferenceDataStore.EdgesTable} WHERE equal_predicate=0""")
        //   .map(rs => rs.long("count")).single.apply().get
        // assert(numEdgesPred0 === 100)


      // }

    }

    // describe("dumping the factor graph") {

    //   it("should work")(pending)
      // {
      //   inferenceDataStore.init()
      //   inferenceDataStore.groundFactorGraph(Map(), Seq(), 0.0, None, false, "", dbSettings, false)

        // // Insert weights
        // SQL("""INSERT INTO dd_graph_weights(id, initial_value, is_fixed, description)
        //   VALUES (0, 0.0, false, 'w1'), (1, 0.0, false, 'w2')""").execute.apply()
        // // Insert variables
        // SQL("""INSERT INTO dd_graph_variables(id, data_type, initial_value, is_evidence, cardinality)
        //   VALUES (0, 'Boolean', 0.0, false, null), (1, 'Boolean', 1.0, true, null), 
        //   (2, 'Multinomial', 3.0, false, 3)""").execute.apply()
        // SQL("""INSERT INTO dd_graph_variables_map(id, variable_id)
        //   VALUES (0, 0), (1, 1), (2,2)""").execute.apply()
        // // Insert factors
        // SQL("""INSERT INTO dd_graph_factors(id, weight_id, factor_function)
        //   VALUES (0, 0, 'ImplyFactorFunction'), (1, 1, 'ImplyFactorFunction')""").execute.apply()
        // // Insert edges
        // SQL("""INSERT INTO dd_graph_edges(factor_id, variable_id, position, is_positive, equal_predicate)
        //   VALUES (0, 0, 0, true, null), (1, 1, 0, true, null), (1, 2, 1, true, null)""").execute.apply()

        // // Dump the factor graph
        // val weightsFile = File.createTempFile("weights", "")
        // val variablesFile = File.createTempFile("variables", "")
        // val factorsFile = File.createTempFile("factors", "")
        // val edgesFile = File.createTempFile("edges", "")
        // val metaFile = File.createTempFile("meta", "csv")
        // val weightsOut = new FileOutputStream(weightsFile)
        // val variablesOut = new FileOutputStream(variablesFile)
        // val factorsOut = new FileOutputStream(factorsFile)
        // val edgesOut = new FileOutputStream(edgesFile)
        // val metaOut = new FileOutputStream(metaFile)
        // val serializer = new BinarySerializer(weightsOut, variablesOut, factorsOut, edgesOut, metaOut)

        // inferenceDataStore.dumpFactorGraph(serializer, Map("r1.c1" -> BooleanType, "r2.c2" -> MultinomialType(5)),
        //   weightsFile.getCanonicalPath, variablesFile.getCanonicalPath, factorsFile.getCanonicalPath,
        //   edgesFile.getCanonicalPath)
        // weightsOut.close()
        // variablesOut.close()
        // factorsOut.close()
        // edgesOut.close()
        // metaOut.close()

        // assert(weightsFile.exists() === true)
        // assert(variablesFile.exists() === true)
        // assert(factorsFile.exists() === true)
        // assert(edgesFile.exists() === true)
      // }

    // }

    describe ("writing back the inference Result") {

      val variablesFile = getClass.getResource("/inference/sample_result.variables.pb").getFile
      val weightsFile = getClass.getResource("/inference/sample_result.weights.pb").getFile
      val schema = Map[String, VariableDataType]("has_spouse.is_true" -> BooleanType)

      it("should work") {
        inferenceDataStore.init()
        inferenceDataStore.groundFactorGraph(Map(), Seq(), 0.0, None, false, "", dbSettings, false)
        SQL(s"""create table has_spouse(id ${inferenceDataStore.keyType} primary key, is_true boolean)""").execute.apply()
        inferenceDataStore.writebackInferenceResult(schema, variablesFile, weightsFile, false)
      }

    }

    describe ("Getting the calibration data") {


      it("should work for Boolean variables") {
        SQL(s"""create table t1_c1_inference(id ${inferenceDataStore.keyType} primary key, c1 boolean, 
          category bigint, expectation double precision)""").execute.apply()
        SQL("""insert into t1_c1_inference(c1, category, expectation) VALUES
          (null, null, 0.31), (null, null, 0.93), (null, null, 0.97), 
          (false, null, 0.0), (true, null, 0.77), (true, null, 0.81)""").execute.apply()
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
         SQL(s"""create table t1_c1_inference(id ${inferenceDataStore.keyType} primary key, c1 bigint, 
          category bigint, expectation double precision)""").execute.apply()
         SQL("""insert into t1_c1_inference(c1, category, expectation) VALUES
          (null, 0, 0.55), (null, 1, 0.55), (null, 2, 0.55), 
          (0, 0, 0.65), (0, 1, 0.95), (0, 2, 0.95), 
          (1, 0, 0.85), (1, 1, 0.95), (1, 2, 0.95)""").execute.apply()
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

