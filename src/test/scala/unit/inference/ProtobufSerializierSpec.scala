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
      assert(graph.getWeightCount === 2)
      assert(graph.getVariableCount === 2)
      assert(graph.getFactorCount === 2)
      assert(graph.getEdgeCount === 3)
    }
  
  }

}