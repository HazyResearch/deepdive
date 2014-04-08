package org.deepdive.test.unit

import java.io._

class SerializationReader(weightsInput: InputStream, variablesInput: InputStream, 
  factorsInput: InputStream, edgesInput: InputStream, metaDataInput: FileReader) {

  val weightStream = new DataInputStream(weightsInput)
  val variableStream = new DataInputStream(variablesInput)
  val factorStream = new DataInputStream(factorsInput)
  val edgeStream = new DataInputStream(edgesInput)
  val metaStream = new BufferedReader(metaDataInput)

  def readWeights : WeightTest = {
    val weight = new WeightTest(
      weightStream.readLong(),
      weightStream.readBoolean(),
      weightStream.readDouble())
    return weight
  }

  def readVariables : VariableTest = {
    val variable = new VariableTest(
      variableStream.readLong(),
      variableStream.readBoolean(),
      variableStream.readDouble(),
      variableStream.readShort(),
      variableStream.readLong(),
      variableStream.readLong())
    return variable
  }

  def readFactors : FactorTest = {
    val factor = new FactorTest(
      factorStream.readLong(),
      factorStream.readLong(),
      factorStream.readShort(),
      factorStream.readLong())
    return factor
  }

  def readEdges : EdgeTest = {
    val edge = new EdgeTest(
      edgeStream.readLong(),
      edgeStream.readLong(),
      edgeStream.readLong(),
      edgeStream.readBoolean(),
      edgeStream.readLong())
    return edge
  }

}