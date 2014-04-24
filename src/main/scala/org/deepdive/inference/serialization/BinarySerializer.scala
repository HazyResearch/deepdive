package org.deepdive.inference

import org.deepdive.Logging
import org.deepdive.settings._
import java.io._

class BinarySerializer(weightsOutput: OutputStream, variablesOutput: OutputStream, 
  factorsOutput: OutputStream, edgesOutput: OutputStream, metaDataOutput: OutputStream) 
  extends Serializer with Logging {

  val weightStream = new DataOutputStream(weightsOutput)
  val variableStream = new DataOutputStream(variablesOutput)
  val factorStream = new DataOutputStream(factorsOutput)
  val edgeStream = new DataOutputStream(edgesOutput)
  val metaStream = new PrintStream(metaDataOutput)



  def addWeight(weightId: Long, isFixed: Boolean, initialValue: Double) : Unit = {
    weightStream.writeLong(weightId)
    weightStream.writeBoolean(isFixed)
    weightStream.writeDouble(initialValue)
    // nodescription
  }

  def addVariable(variableId: Long, isEvidence: Boolean, initialValue: Double, 
    dataType: String, edgeCount: Long, cardinality: Long) : Unit = {
    val variableDataType = dataType match {
      case "Boolean" => 0
      case "Multinomial" => 1
    }
    variableStream.writeLong(variableId)
    variableStream.writeBoolean(isEvidence)
    variableStream.writeDouble(initialValue)
    variableStream.writeShort(variableDataType)
    variableStream.writeLong(edgeCount)  
    variableStream.writeLong(cardinality)  
  }

  def addFactor(factorId: Long, weightId: Long, factorFunction: String, edgeCount: Long) : Unit = {
    val factorFunctionType = factorFunction match {
      case "ImplyFactorFunction" => 0
      case "OrFactorFunction" => 1
      case "AndFactorFunction" => 2
      case "EqualFactorFunction" => 3
      case "IsTrueFactorFunction" =>  4
    }
    factorStream.writeLong(factorId)
    factorStream.writeLong(weightId)
    factorStream.writeShort(factorFunctionType)
    factorStream.writeLong(edgeCount)
  }


  def addEdge(variableId: Long, factorId: Long, position: Long, isPositive: Boolean, 
    equalPredicate: Long) : Unit = {
    edgeStream.writeLong(variableId)
    edgeStream.writeLong(factorId)
    edgeStream.writeLong(position)
    edgeStream.writeBoolean(isPositive)
    edgeStream.writeLong(equalPredicate)
  }

  def writeMetadata(numWeights: Long, numVariables: Long, numFactors: Long, numEdges: Long,
    weightsFile: String, variablesFile: String, factorsFile: String, edgesFile: String) : Unit = {
    val out = List(numWeights, numVariables, numFactors, numEdges, weightsFile, variablesFile, 
      factorsFile, edgesFile).mkString(",")
    metaStream.print(out)
  }

  def close() : Unit = {
    weightsOutput.flush()
    variablesOutput.flush()
    factorsOutput.flush()
    edgesOutput.flush()
    metaDataOutput.flush()
  }

}