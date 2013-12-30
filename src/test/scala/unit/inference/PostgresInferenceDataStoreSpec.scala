package org.deepdive.test

import anorm._
import java.io.File
import org.deepdive.inference._
import org.scalatest._
import scala.io.Source

class PostgresInferenceDataStoreSpec extends FunSpec with BeforeAndAfter
  with PostgresInferenceDataStoreComponent {

  lazy implicit val connection = inferenceDataStore.connection

  /* Initialize and clear the data store before each test */
  before {
    PostgresTestDataStore.init()
    SQL("drop schema if exists public cascade; create schema public;").execute()
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
        inferenceDataStore.addVariable(Variable(0, VariableDataType.Boolean, 0.0, true, false, "r1", "c1"))
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
        inferenceDataStore.addFactor(Factor(0, "someFactorFunction", 0, List[FactorVariable](
          FactorVariable(0,0,true,0))))
      }
    }

    describe("flushing the factor graph") {
      it("should work") {
        val variables = (1 to 100).map { variableId =>
          Variable(variableId, VariableDataType.Boolean, 0.0, true, false, "r1", "c1")
        }.toList
        
        val weights = (1 to 10).map { weightId => 
          Weight(weightId, 0.0, false, "someWeight")
        }.toList
        
        val factors = (1 to 10).map { factorId =>
          Factor(factorId, "someFactorFunction", 0, List[FactorVariable](FactorVariable(factorId,0,true,0)))
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
        val variable1 = Variable(0, VariableDataType.Boolean, 0.0, true, false, "r1", "c1")
        val variable2 = variable1.copy(id=1)
        val variable3 = variable1.copy(id=2)
        val weight1 = Weight(0, 0.0, false, "someWeight")
        val weight2 = Weight(1, 0.0, false, "someWeight")
        val factor1 =  Factor(0, "someFactorFunction", 0, 
          List[FactorVariable](FactorVariable(0,0,true,0)))
        val factor2 =  Factor(1, "someFactorFunction", 0, 
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

        val variablesFile = File.createTempFile("variables", "")
        val factorsFile = File.createTempFile("factors", "")
        val weightsFile = File.createTempFile("weights", "")
        inferenceDataStore.dumpFactorGraph(variablesFile, factorsFile, weightsFile)

        assert(Source.fromFile(variablesFile).getLines.size == 3)
        assert(Source.fromFile(factorsFile).getLines.size == 2)
        assert(Source.fromFile(weightsFile).getLines.size == 2)
      }

      it("should work when we add variables, weight, or factors several times") {
         addSampleData()

        // Add the same data again, make sure it does not appear in the output
        val variable1 = Variable(0, VariableDataType.Boolean, 0.0, true, false, "r1", "c1")
        val weight1 = Weight(0, 0.0, false, "someWeight")
        val factor1 =  Factor(0, "someFactorFunction", 0, 
          List[FactorVariable](FactorVariable(0,0,true,0)))
        inferenceDataStore.addVariable(variable1)
        inferenceDataStore.addWeight(weight1)
        inferenceDataStore.addFactor(factor1)

        val variablesFile = File.createTempFile("variables", "")
        val factorsFile = File.createTempFile("factors", "")
        val weightsFile = File.createTempFile("weights", "")
        inferenceDataStore.dumpFactorGraph(variablesFile, factorsFile, weightsFile)

        assert(Source.fromFile(variablesFile).getLines.size == 3)
        assert(Source.fromFile(factorsFile).getLines.size == 2)
        assert(Source.fromFile(weightsFile).getLines.size == 2)

      }

    }
    
  }

}
