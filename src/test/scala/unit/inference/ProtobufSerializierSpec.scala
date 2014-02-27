package org.deepdive.test.unit

import java.io.{File, FileInputStream, FileOutputStream}
import org.deepdive.inference.ProtobufSerializer
import org.deepdive.serialization.FactorGraphProtos
import org.scalatest._
import org.deepdive.settings._

class ProtobufSerializerSpec extends FunSpec with BeforeAndAfter {

  describe("Serializing and deserializing") {

    it("should work") {
      // Dump a factor graph
      val weightsFile = File.createTempFile("weights", "pb")
      val variablesFile = File.createTempFile("variables", "pb")
      val factorsFile = File.createTempFile("factors", "pb")
      val edgesFile = File.createTempFile("edges", "pb")
      val oWeights = new FileOutputStream(weightsFile)
      val oVariables = new FileOutputStream(variablesFile)
      val oFactors = new FileOutputStream(factorsFile)
      val oEdges = new FileOutputStream(edgesFile)

      val serializier = new ProtobufSerializer(oWeights, oVariables, oFactors, oEdges)
      serializier.addWeight(0, false, 0.0, "someFeature1")
      serializier.addWeight(1, true, 10.0, "someFeature2")
      serializier.addVariable(0, None, BooleanType, None)
      serializier.addVariable(1, None, BooleanType, None)
      serializier.addFactor(0, 0, "IsTrueFactorFunction")
      serializier.addFactor(1, 1, "ImplyFactorFunction")
      serializier.addEdge(0, 0, 0, true)
      serializier.addEdge(0, 1, 0, true)
      serializier.addEdge(1, 1, 1, false)
      serializier.close()

      oWeights.close()
      oVariables.close()
      oFactors.close()
      oEdges.close()

      // Read the factor graph
      val weightsInput = new FileInputStream(weightsFile)
      val variablesInput = new FileInputStream(variablesFile)
      val factorsInput = new FileInputStream(factorsFile)
      val edgesInput = new FileInputStream(edgesFile)
      val weights = List(1,2).map(i => FactorGraphProtos.Weight.parseDelimitedFrom(weightsInput))
      val variables = List(1,2).map(i => FactorGraphProtos.Variable.parseDelimitedFrom(variablesInput))
      val factors = List(1,2).map(i => FactorGraphProtos.Factor.parseDelimitedFrom(factorsInput))
      val edges = List(1,2,3).map(i => FactorGraphProtos.GraphEdge.parseDelimitedFrom(edgesInput))

      assert(weights(0).getId === 0)
      assert(weights(0).getIsFixed === false)
      assert(weights(0).getInitialValue === 0.0)
      assert(weights(0).getDescription === "someFeature1")
      assert(weights(1).getId === 1)
      assert(weights(1).getIsFixed === true)
      assert(weights(1).getInitialValue === 10.0)
      assert(weights(1).getDescription === "someFeature2")

      assert(variables(0).getId === 0)
      assert(variables(0).hasInitialValue === false)
      assert(variables(0).getDataType === FactorGraphProtos.Variable.VariableDataType.BOOLEAN)
      assert(variables(1).getId === 1)
      assert(variables(1).hasInitialValue === false)
      assert(variables(1).getDataType === FactorGraphProtos.Variable.VariableDataType.BOOLEAN)

      assert(factors(0).getId === 0)
      assert(factors(0).getWeightId === 0)
      assert(factors(0).getFactorFunction === FactorGraphProtos.Factor.FactorFunctionType.ISTRUE)
      assert(factors(1).getId === 1)
      assert(factors(1).getWeightId === 1)
      assert(factors(1).getFactorFunction === FactorGraphProtos.Factor.FactorFunctionType.IMPLY)

      assert(edges(0).getVariableId === 0)
      assert(edges(0).getFactorId === 0)
      assert(edges(0).getPosition === 0)
      assert(edges(0).getIsPositive === true)
      assert(edges(1).getVariableId === 0)
      assert(edges(1).getFactorId === 1)
      assert(edges(1).getPosition === 0)
      assert(edges(1).getIsPositive === true)
      assert(edges(2).getVariableId === 1)
      assert(edges(2).getFactorId === 1)
      assert(edges(2).getPosition === 1)
      assert(edges(2).getIsPositive === false)

    }
  
  }

}