package org.deepdive.test.unit

import java.io._
import org.deepdive.inference.BinarySerializer
import org.scalatest._
import org.deepdive.settings._

class BinarySerializerSpec extends FunSpec with BeforeAndAfter {

  describe("Serializing and deserializing") {

    it("should work") {
      // Dump a factor graph
      val weightsFile = File.createTempFile("weights", "")
      val variablesFile = File.createTempFile("variables", "")
      val factorsFile = File.createTempFile("factors", "")
      val edgesFile = File.createTempFile("edges", "")
      val metaFile = File.createTempFile("meta", "csv")
      val oWeights = new FileOutputStream(weightsFile)
      val oVariables = new FileOutputStream(variablesFile)
      val oFactors = new FileOutputStream(factorsFile)
      val oEdges = new FileOutputStream(edgesFile)
      val oMeta = new FileOutputStream(metaFile)

      val serializier = new BinarySerializer(oWeights, oVariables, oFactors, oEdges, oMeta)
      serializier.addWeight(0, false, 0.0)
      serializier.addWeight(1, true, 10.0)
      serializier.addVariable(0, false, 0.0, "Boolean", 2, 0)
      serializier.addVariable(1, true, 1.0, "Boolean", 1, 0)
      serializier.addVariable(2, false, 0.0, "Multinomial", 0, 3)
      serializier.addFactor(0, 0, "IsTrueFactorFunction", 1)
      serializier.addFactor(1, 1, "ImplyFactorFunction", 2)
      serializier.addEdge(0, 0, 0, true, 0)
      serializier.addEdge(0, 1, 0, true, 0)
      serializier.addEdge(1, 1, 1, false, 3)
      serializier.writeMetadata(2, 3, 2, 3, weightsFile.getCanonicalPath, variablesFile.getCanonicalPath,
        factorsFile.getCanonicalPath, edgesFile.getCanonicalPath)
      serializier.close()

      oWeights.close()
      oVariables.close()
      oFactors.close()
      oEdges.close()
      oMeta.close()

      assert(true)

      // Read the factor graph
      val weightsInput = new FileInputStream(weightsFile)
      val variablesInput = new FileInputStream(variablesFile)
      val factorsInput = new FileInputStream(factorsFile)
      val edgesInput = new FileInputStream(edgesFile)
      val metaDataInput = new FileReader(metaFile)
      val reader = new SerializationReader(weightsInput, variablesInput, factorsInput,
        edgesInput, metaDataInput)
      val weights = List(1,2).map(i => reader.readWeights)
      val variables = List(1,2,3).map(i => reader.readVariables)
      val factors = List(1,2).map(i => reader.readFactors)
      val edges = List(1,2,3).map(i => reader.readEdges)
      // val meta = FactorGraphProtos.FactorGraph.parseFrom(metaDataInput)

      assert(weights(0).id === 0)
      assert(weights(0).isFixed === false)
      assert(weights(0).initialValue === 0.0)
      assert(weights(1).id === 1)
      assert(weights(1).isFixed === true)
      assert(weights(1).initialValue === 10.0)

      assert(variables(0).id === 0)
      assert(variables(0).dataType === 0)
      assert(variables(0).edgeCount === 2)
      assert(variables(0).isEvidence === false)
      assert(variables(1).id === 1)
      assert(variables(1).isEvidence === true)
      assert(variables(1).initialValue === 1.0)
      assert(variables(1).dataType === 0)
      assert(variables(1).edgeCount === 1)
      assert(variables(2).dataType === 1)
      assert(variables(2).cardinality === 3)

      assert(factors(0).factorId === 0)
      assert(factors(0).weightId === 0)
      assert(factors(0).edgeCount === 1)
      assert(factors(0).factorFunction === 4)
      assert(factors(1).factorId === 1)
      assert(factors(1).weightId === 1)
      assert(factors(1).edgeCount === 2)
      assert(factors(1).factorFunction === 0)

      assert(edges(0).variableId === 0)
      assert(edges(0).factorId === 0)
      assert(edges(0).position === 0)
      assert(edges(0).isPositive === true)
      assert(edges(1).variableId === 0)
      assert(edges(1).factorId === 1)
      assert(edges(1).position === 0)
      assert(edges(1).isPositive === true)
      assert(edges(2).variableId === 1)
      assert(edges(2).factorId === 1)
      assert(edges(2).position === 1)
      assert(edges(2).isPositive === false)

      // assert(meta.getNumWeights == 2)

    }
  
  }

}