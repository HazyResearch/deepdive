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



  def addWeight(weightId: Long, isFixed: Boolean, initialValue: Double, weightLength: Long) : Unit = {
    weightStream.writeLong(weightId)
    weightStream.writeBoolean(isFixed)
    weightStream.writeDouble(initialValue)
    weightStream.writeLong(weightLength)
    // nodescription
  }

  def addVariable(variableId: Long, isEvidence: Boolean, initialValue: Double) : Unit = {
    variableStream.writeLong(variableId)
    variableStream.writeBoolean(isEvidence)
    variableStream.writeDouble(initialValue)
  }

  def addFactor(factorId: Long, factorFunction: String) : Unit = {
    val factorFunctionType = factorFunction match {
      case "ImplyFactorFunction" => 0
      case "OrFactorFunction" => 1
      case "AndFactorFunction" => 2
      case "EqualFactorFunction" => 3
      case "IsTrueFactorFunction" =>  4
      case "ConvolutionFactorFunction" => 1000
      case "SamplingFactorFunction" => 1001
      case "LikelihoodFactorFunction" => 1010
      case "LeastSquaresFactorFunction" => 1011
      case "SoftmaxFactorFunction" => 1020
    }
    factorStream.writeLong(factorId)
    factorStream.writeShort(factorFunctionType)
  }


  def addEdge(variableId: Long, factorId: Long, weightId: Long, position: Long) : Unit = {
    edgeStream.writeLong(variableId)
    edgeStream.writeLong(factorId)
    edgeStream.writeLong(weightId)
    edgeStream.writeLong(position)
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