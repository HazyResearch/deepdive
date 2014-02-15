package org.deepdive.test.unit

import java.io.{File, FileInputStream}
import org.deepdive.inference.ProtobufSerializer
import org.deepdive.serialization.FactorGraphProtos
import org.scalatest._

class ProtobufSerializerSpec extends FunSpec with BeforeAndAfter {

  describe("Serializing and deserializing") {

    it("should work") {
      // Dump a factor graph
      val outputFile = File.createTempFile("ProtobufSerializerSpec", "pb")
      val serializier = new ProtobufSerializer
      serializier.addWeight(0, false, 0.0, "someFeature1")
      serializier.addWeight(1, true, 10.0, "someFeature2")
      serializier.addVariable(0, None, "Boolean")
      serializier.addVariable(1, None, "Boolean")
      serializier.addFactor(0, 0, "IsTrueFactorFunction")
      serializier.addFactor(1, 1, "ImplyFactorFunction")
      serializier.addEdge(0, 0, 0, true)
      serializier.addEdge(0, 1, 0, true)
      serializier.addEdge(1, 1, 1, false)
      serializier.write(outputFile)

      // Read the factor graph
      val graph = FactorGraphProtos.FactorGraph.newBuilder
      graph.mergeFrom(new FileInputStream(outputFile))

      assert(graph.getWeight(0).getId === 0)
      assert(graph.getWeight(0).getIsFixed === false)
      assert(graph.getWeight(0).getInitialValue === 0.0)
      assert(graph.getWeight(0).getDescription === "someFeature1")
      assert(graph.getWeight(1).getId === 1)
      assert(graph.getWeight(1).getIsFixed === true)
      assert(graph.getWeight(1).getInitialValue === 10.0)
      assert(graph.getWeight(1).getDescription === "someFeature2")

      assert(graph.getVariable(0).getId === 0)
      assert(graph.getVariable(0).hasInitialValue === false)
      assert(graph.getVariable(0).getDataType === FactorGraphProtos.Variable.VariableDataType.BOOLEAN)
      assert(graph.getVariable(1).getId === 1)
      assert(graph.getVariable(1).hasInitialValue === false)
      assert(graph.getVariable(1).getDataType === FactorGraphProtos.Variable.VariableDataType.BOOLEAN)

      assert(graph.getFactor(0).getId === 0)
      assert(graph.getFactor(0).getWeightId === 0)
      assert(graph.getFactor(0).getFactorFunction === FactorGraphProtos.Factor.FactorFunctionType.ISTRUE)
      assert(graph.getFactor(1).getId === 1)
      assert(graph.getFactor(1).getWeightId === 1)
      assert(graph.getFactor(1).getFactorFunction === FactorGraphProtos.Factor.FactorFunctionType.IMPLY)

      assert(graph.getEdge(0).getVariableId === 0)
      assert(graph.getEdge(0).getFactorId === 0)
      assert(graph.getEdge(0).getPosition === 0)
      assert(graph.getEdge(0).getIsPositive === true)
      assert(graph.getEdge(1).getVariableId === 0)
      assert(graph.getEdge(1).getFactorId === 1)
      assert(graph.getEdge(1).getPosition === 0)
      assert(graph.getEdge(1).getIsPositive === true)
      assert(graph.getEdge(2).getVariableId === 1)
      assert(graph.getEdge(2).getFactorId === 1)
      assert(graph.getEdge(2).getPosition === 1)
      assert(graph.getEdge(2).getIsPositive === false)

    }
  
  }

}