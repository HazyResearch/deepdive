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

  val dbSettings = DbSettings(null, null, System.getenv("PGUSER"), null, System.getenv("DBNAME"), 
    System.getenv("PGHOST"), System.getenv("PGPORT"), null, null, null)

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
  
  /**********************
   * Note id must not be the first column,
   * since the first column is the distribution key in greenplum,
   * and DeepDive will try to do update id, which is not allowed 
   * on distribution key.
   **********************/
  describe("Postgres inference data store") {
    
    describe("intializing") {
      it("should work") {
        inferenceDataStore.init()
      }
    }

    describe("grounding the factor graph with Boolean variables") {
      
      it("should work with fixed weight") {
        inferenceDataStore.init()
        
        // Insert sample data
        SQL(s"""CREATE TABLE r1(weight ${inferenceDataStore.stringType},
          is_correct boolean, id bigint);""").execute.apply()        
        val data = (1 to 100).map { i =>
          Map("id" -> i, "is_correct" -> s"${i%2==0}".toBoolean)
        }
        dataStoreHelper.bulkInsert("r1", data.iterator)

        val schema = Map[String, VariableDataType]("r1.is_correct" -> BooleanType)

        // Build the factor description
        val factorDesc = FactorDesc("testFactor", 
            """SELECT id AS "r1.id", is_correct AS "r1.is_correct" FROM r1""", 
          IsTrueFactorFunction(Seq("r1.is_correct")), 
          KnownFactorWeight(0.37), "weight_prefix")
        val holdoutFraction = 0.0

        // Ground the graph
        inferenceDataStore.groundFactorGraph(schema, Seq(factorDesc), holdoutFraction, None, false, "", dbSettings, false)

        // Check the result
        val numWeights = SQL(s"""SELECT COUNT(*) AS "count" FROM ${inferenceDataStore.WeightsTable}""")
          .map(rs => rs.long("count")).single.apply().get
        assert(numWeights === 1)
        val initValue = SQL(s"""SELECT initvalue FROM ${inferenceDataStore.WeightsTable} limit 1""")
          .map(rs => rs.double("initvalue")).single.apply().get
        assert(initValue === 0.37)
        val isFixed = SQL(s"""SELECT isfixed FROM ${inferenceDataStore.WeightsTable} limit 1""")
          .map(rs => rs.int("isfixed")).single.apply().get
        assert(isFixed === 1)
        val weightDesc = SQL(s"""SELECT description FROM ${inferenceDataStore.WeightsTable} limit 1""")
          .map(rs => rs.string("description")).single.apply().get
        assert(weightDesc === "weight_prefix-")

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
      
      it("should work with weight to learn without weight variables") {
        inferenceDataStore.init()
        
        // Insert sample data
        SQL(s"""CREATE TABLE r1(weight ${inferenceDataStore.stringType},
          is_correct boolean, id bigint);""").execute.apply()        
        val data = (1 to 100).map { i =>
          Map("id" -> i, "is_correct" -> s"${i%2==0}".toBoolean)
        }
        dataStoreHelper.bulkInsert("r1", data.iterator)

        val schema = Map[String, VariableDataType]("r1.is_correct" -> BooleanType)

        // Build the factor description
        val factorDesc = FactorDesc("testFactor", 
            """SELECT id AS "r1.id", is_correct AS "r1.is_correct" FROM r1""", 
          IsTrueFactorFunction(Seq("r1.is_correct")), 
          UnknownFactorWeight(List()), "weight_prefix")
        val holdoutFraction = 0.0

        // Ground the graph
        inferenceDataStore.groundFactorGraph(schema, Seq(factorDesc), holdoutFraction, None, false, "", dbSettings, false)

        // Check the result
        val numWeights = SQL(s"""SELECT COUNT(*) AS "count" FROM ${inferenceDataStore.WeightsTable}""")
          .map(rs => rs.long("count")).single.apply().get
        assert(numWeights === 1)
        val initValue = SQL(s"""SELECT initvalue FROM ${inferenceDataStore.WeightsTable} limit 1""")
          .map(rs => rs.double("initvalue")).single.apply().get
        assert(initValue === 0)
        val isFixed = SQL(s"""SELECT isfixed FROM ${inferenceDataStore.WeightsTable} limit 1""")
          .map(rs => rs.int("isfixed")).single.apply().get
        assert(isFixed === 0)
        val weightDesc = SQL(s"""SELECT description FROM ${inferenceDataStore.WeightsTable} limit 1""")
          .map(rs => rs.string("description")).single.apply().get
        assert(weightDesc === "weight_prefix-")

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
        val weightDesc = SQL(s"""SELECT description FROM ${inferenceDataStore.WeightsTable} WHERE description LIKE '%71'""")
          .map(rs => rs.string("description")).single.apply().get
        assert(weightDesc === "weight_prefix-weight_71")

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
        SQL(s"""CREATE TABLE r1(weight int,
          is_correct boolean, id bigint);""").execute.apply()        
        val data = (1 to 100).map { i =>
          Map("id" -> i, "weight" -> i, "is_correct" -> s"${i%2==0}".toBoolean)
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
          SELECT id FROM r1 WHERE weight <= 10;"""
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
        intercept[RuntimeException] {
          inferenceDataStore.groundFactorGraph(schema, Seq(factorDesc), holdoutFraction, None, false, "", dbSettings, false)
        }

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

      it("should work with weight variables that are null")
      {
        inferenceDataStore.init()
        
        // Create table with multinomial data
        SQL(s"""CREATE TABLE r1(weight text,
          value bigint, id bigint);""").execute.apply() 
        val data = (1 to 100).map { i =>
          Map("id" -> i, "weight" -> s"weight_${i}", "value" -> (i%4))
        }
        dataStoreHelper.bulkInsert("r1", data.iterator)

        // Define the schema
        val schema = Map[String, VariableDataType]("r1.value" -> MultinomialType(4))

        // Build the factor description
        val factorDesc = FactorDesc("testFactor", 
          """SELECT id AS "r1.id", weight AS "weight", value AS "r1.value" FROM r1""", 
          MultinomialFactorFunction(Seq("r1.value")), 
          UnknownFactorWeight(List()), "weight_prefix")
        val holdoutFraction = 0.0

        // Ground the graph
        inferenceDataStore.groundFactorGraph(schema, Seq(factorDesc), holdoutFraction, None, false, "", dbSettings, false)

        val numWeights = SQL(s"""SELECT COUNT(*) AS "count" FROM ${inferenceDataStore.WeightsTable}""")
          .map(rs => rs.long("count")).single.apply().get
        assert(numWeights === 4)

        // One factor for each possible predicate assignment
        val numFactors = SQL(s"""SELECT COUNT(*) AS "count" FROM dd_query_testFactor""")
          .map(rs => rs.long("count")).single.apply().get
        assert(numFactors === 100)

      }

      it("should work with weight variables")
      {
        inferenceDataStore.init()
        
        // Create table with multinomial data
        SQL(s"""CREATE TABLE r1(weight text,
          value bigint, id bigint);""").execute.apply() 
        val data = (1 to 100).map { i =>
          Map("id" -> i, "weight" -> s"weight_${i%4}", "value" -> (i%4))
        }
        dataStoreHelper.bulkInsert("r1", data.iterator)

        // Define the schema
        val schema = Map[String, VariableDataType]("r1.value" -> MultinomialType(4))

        // Build the factor description
        val factorDesc = FactorDesc("testFactor", 
          """SELECT id AS "r1.id", weight AS "weight", value AS "r1.value" FROM r1""", 
          MultinomialFactorFunction(Seq("r1.value")), 
          UnknownFactorWeight(List("weight")), "weight_prefix")
        val holdoutFraction = 0.0

        // Ground the graph
        inferenceDataStore.groundFactorGraph(schema, Seq(factorDesc), holdoutFraction, None, false, "", dbSettings, false)

        val numWeights = SQL(s"""SELECT COUNT(*) AS "count" FROM ${inferenceDataStore.WeightsTable}""")
          .map(rs => rs.long("count")).single.apply().get
        assert(numWeights === 16)

        // One factor for each possible predicate assignment
        val numFactors = SQL(s"""SELECT COUNT(*) AS "count" FROM dd_query_testFactor""")
          .map(rs => rs.long("count")).single.apply().get
        assert(numFactors === 100)

      }

      it("should work with fixed weight")
      {
        inferenceDataStore.init()
        
        // Create table with multinomial data
        SQL(s"""CREATE TABLE r1(weight text,
          value bigint, id bigint);""").execute.apply() 
        val data = (1 to 100).map { i =>
          Map("id" -> i, "value" -> (i%4))
        }
        dataStoreHelper.bulkInsert("r1", data.iterator)

        // Define the schema
        val schema = Map[String, VariableDataType]("r1.value" -> MultinomialType(4))

        // Build the factor description
        val factorDesc = FactorDesc("testFactor", 
          """SELECT id AS "r1.id", value AS "r1.value" FROM r1""", 
          MultinomialFactorFunction(Seq("r1.value")), 
          KnownFactorWeight(0.37), "weight_prefix")
        val holdoutFraction = 0.0

        // Ground the graph
        inferenceDataStore.groundFactorGraph(schema, Seq(factorDesc), holdoutFraction, None, false, "", dbSettings, false)

        val numWeights = SQL(s"""SELECT COUNT(*) AS "count" FROM ${inferenceDataStore.WeightsTable}""")
          .map(rs => rs.long("count")).single.apply().get
        assert(numWeights === 4)

        // TODO: what does cardinality mean in this table?

        // For fixed weights, all rows should have same initvalue          
        assert(SQL(s"""SELECT initvalue FROM ${inferenceDataStore.WeightsTable} limit 1""")
          .map(rs => rs.double("initvalue")).single.apply().get === 0.37)
        assert(SQL(s"""SELECT isfixed FROM ${inferenceDataStore.WeightsTable} limit 1""")
          .map(rs => rs.int("isfixed")).single.apply().get === 1)

        // One factor for each possible predicate assignment
        val numFactors = SQL(s"""SELECT COUNT(*) AS "count" FROM dd_query_testFactor""")
          .map(rs => rs.long("count")).single.apply().get
        assert(numFactors === 100)

      }

      it("should work with weight to learn without weight variables")
      {
        inferenceDataStore.init()
        
        // Create table with multinomial data
        SQL(s"""CREATE TABLE r1(weight text,
          value bigint, id bigint);""").execute.apply() 
        val data = (1 to 100).map { i =>
          Map("id" -> i, "value" -> (i%4))
        }
        dataStoreHelper.bulkInsert("r1", data.iterator)

        // Define the schema
        val schema = Map[String, VariableDataType]("r1.value" -> MultinomialType(4))

        // Build the factor description
        val factorDesc = FactorDesc("testFactor", 
          """SELECT id AS "r1.id", value AS "r1.value" FROM r1""", 
          MultinomialFactorFunction(Seq("r1.value")), 
          UnknownFactorWeight(List()), "weight_prefix")
        val holdoutFraction = 0.0

        // Ground the graph
        inferenceDataStore.groundFactorGraph(schema, Seq(factorDesc), holdoutFraction, None, false, "", dbSettings, false)

        val numWeights = SQL(s"""SELECT COUNT(*) AS "count" FROM ${inferenceDataStore.WeightsTable}""")
          .map(rs => rs.long("count")).single.apply().get
        assert(numWeights === 4)

        assert(SQL(s"""SELECT isfixed FROM ${inferenceDataStore.WeightsTable} limit 1""")
          .map(rs => rs.int("isfixed")).single.apply().get === 0)

        // One factor for each possible predicate assignment
        val numFactors = SQL(s"""SELECT COUNT(*) AS "count" FROM dd_query_testFactor""")
          .map(rs => rs.long("count")).single.apply().get
        assert(numFactors === 100)

      }

    }

    describe ("writing back the inference Result") {

      val variablesFile = getClass.getResource("/inference/sample_result.variables.text").getFile
      val weightsFile = getClass.getResource("/inference/sample_result.weights.text").getFile
      val schema = Map[String, VariableDataType]("has_spouse.is_true" -> BooleanType)

      it("should work") {
        inferenceDataStore.init()
        inferenceDataStore.groundFactorGraph(Map(), Seq(), 0.0, None, false, "", dbSettings, false)
        SQL(s"""create table has_spouse(id ${inferenceDataStore.keyType} primary key, is_true boolean)""").execute.apply()
        inferenceDataStore.writebackInferenceResult(schema, variablesFile, weightsFile, false, dbSettings)
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

