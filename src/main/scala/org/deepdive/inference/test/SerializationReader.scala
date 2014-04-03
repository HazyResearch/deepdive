package org.deepdive.inference.test

import java.io._

class SerializationReader(weightsInput: InputStream, variablesInput: InputStream, 
  factorsInput: InputStream, edgesInput: InputStream, metaDataInput: FileReader) {

  val weightStream = new DataInputStream(weightsInput)
  val variableStream = new DataInputStream(variablesInput)
  val factorStream = new DataInputStream(factorsInput)
  val edgeStream = new DataInputStream(edgesInput)
  val metaStream = new BufferedReader(metaDataInput)

  def readWeights : org.deepdive.inference.test.Weight = {
    val weight = new org.deepdive.inference.test.Weight(
      weightStream.readLong(),
      weightStream.readBoolean(),
      weightStream.readDouble())
    return weight
  }

  def readVariables : org.deepdive.inference.test.Variable = {
    val variable = new org.deepdive.inference.test.Variable(
      variableStream.readLong(),
      variableStream.readBoolean(),
      variableStream.readDouble(),
      variableStream.readShort(),
      variableStream.readLong(),
      variableStream.readLong())
    return variable
  }

  def readFactors : org.deepdive.inference.test.Factor = {
    val Factor = new org.deepdive.inference.test.Factor(
      factorStream.readLong(),
      factorStream.readLong(),
      factorStream.readShort(),
      factorStream.readLong())
    return Factor
  }

  def readEdges : org.deepdive.inference.test.Edge = {
    val Edge = new org.deepdive.inference.test.Edge(
      edgeStream.readLong(),
      edgeStream.readLong(),
      edgeStream.readLong(),
      edgeStream.readBoolean(),
      edgeStream.readLong())
    return Edge
  }

}