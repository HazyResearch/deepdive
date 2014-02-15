package org.deepdive.test.unit

import anorm._
import java.io.File
import org.deepdive.calibration._
import org.deepdive.inference._
import org.deepdive.test._
import org.scalatest._
import org.deepdive.datastore._
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

    describe("adding variables") {
      it("should work") {
        inferenceDataStore.init()
        inferenceDataStore.addVariable(Variable(0, VariableDataType.Boolean, Option(0.0), true, false, "r1", "c1", 0))
      }
    }

    describe("adding weights") {
      it("should work") {
        inferenceDataStore.init()
        inferenceDataStore.addWeight(Weight(0, 0.0, false, "someWeight"))
      }
    }

    describe("adding factors") {
      it("should work") {
        inferenceDataStore.init()
        inferenceDataStore.addFactor(Factor(0, "ImplyFactorFunction", 0, List[FactorVariable](
          FactorVariable(0,0,true,0))))
      }
    }

    describe("flushing the factor graph") {
      it("should work") {
        val variables = (1 to 100).map { variableId =>
          Variable(variableId, VariableDataType.Boolean, Option(0.0), true, false, "r1", "c1", variableId)
        }.toList
        
        val weights = (1 to 10).map { weightId => 
          Weight(weightId, 0.0, false, "someWeight")
        }.toList
        
        val factors = (1 to 10).map { factorId =>
          Factor(factorId, "ImplyFactorFunction", 0, List[FactorVariable](FactorVariable(factorId,0,true,0)))
        }.toList

        inferenceDataStore.init()
        variables.foreach(inferenceDataStore.addVariable)
        weights.foreach(inferenceDataStore.addWeight)
        factors.foreach(inferenceDataStore.addFactor)
        inferenceDataStore.flush()

        val numVariables = SQL("select count(*) from variables")().head[Long]("count")
        val numWeights = SQL("select count(*) from factors")().head[Long]("count")
        val numFactors = SQL("select count(*) from weights")().head[Long]("count")
        val numFactorVariables = SQL("select count(*) from factor_variables")().head[Long]("count")

        assert(numVariables == 100)
        assert(numWeights == 10)
        assert(numFactors == 10)
        assert(numFactorVariables == 10)
      }
    }

    describe("dumping the factor graph") {

      def addSampleData() = {
        val variable1 = Variable(0, VariableDataType.Boolean, Option(0.0), true, false, "r1", "c1", 0)
        val variable2 = variable1.copy(id=1)
        val variable3 = variable1.copy(id=2)
        val weight1 = Weight(0, 0.0, false, "someWeight")
        val weight2 = Weight(1, 0.0, false, "someWeight")
        val factor1 =  Factor(0, "ImplyFactorFunction", 0, 
          List[FactorVariable](FactorVariable(0,0,true,0)))
        val factor2 =  Factor(1, "ImplyFactorFunction", 0, 
          List[FactorVariable](FactorVariable(1,0,true,1), FactorVariable(2,0,true,2)))

        inferenceDataStore.init()
        inferenceDataStore.addVariable(variable1)
        inferenceDataStore.addVariable(variable2)
        inferenceDataStore.addVariable(variable3)
        inferenceDataStore.addWeight(weight1)
        inferenceDataStore.addWeight(weight2)
        inferenceDataStore.addFactor(factor1)
        inferenceDataStore.addFactor(factor2)
        inferenceDataStore.flush()
      }

      it("should work with unique variables, weights, and factors") {
        addSampleData()

        val serializier = new ProtobufSerializer
        val graphDumpFile = File.createTempFile("factorGraph", "pg")
        inferenceDataStore.dumpFactorGraph(serializier, graphDumpFile)

        assert(graphDumpFile.exists == true)
      }

      it("should work when we add variables, weight, or factors several times") {
         addSampleData()

        // Add the same data again, make sure it does not appear in the output
        val variable1 = Variable(0, VariableDataType.Boolean, Option(0.0), true, false, "r1", "c1", 0)
        val weight1 = Weight(0, 0.0, false, "someWeight")
        val factor1 =  Factor(0, "ImplyFactorFunction", 0, 
          List[FactorVariable](FactorVariable(0,0,true,0)))
        inferenceDataStore.addVariable(variable1)
        inferenceDataStore.addWeight(weight1)
        inferenceDataStore.addFactor(factor1)

        val serializier = new ProtobufSerializer
        val graphDumpFile = File.createTempFile("factorGraph", "pg")
        inferenceDataStore.dumpFactorGraph(serializier, graphDumpFile)

        assert(graphDumpFile.exists == true)
      }

    }

    describe ("writing back the inference Result") {

      it("should work")(pending)

    }

    describe ("Getting the calibration data") {

      def createSampleInferenceRelation() {
        SQL("""create table t1_c1_inference(id bigserial primary key, c1 boolean, 
          last_sample boolean, probability double precision)""").execute()
        SQL("""insert into t1_c1_inference(c1, last_sample, probability) VALUES
          (null, false, 0.31), (null, true, 0.93), (null, true, 0.97), 
          (false, false, 0.0), (true, true, 0.77), (true, true, 0.81)""").execute()
      }

      it("should work") {
        createSampleInferenceRelation()
        val buckets = Bucket.ten
        val result = inferenceDataStore.getCalibrationData("t1.c1", buckets)
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

    }
    
  }

}
