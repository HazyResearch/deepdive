package org.deepdive.inference

import org.deepdive.Logging
import org.deepdive.settings._
import org.deepdive.serialization.FactorGraphProtos
import java.io.OutputStream

class ProtobufSerializer(weightsOuput: OutputStream, variablesOutput: OutputStream, 
  factorsOutput: OutputStream, edgesOutput: OutputStream, metaDataOutput: OutputStream) extends Serializer with Logging {

  val factorGraphbuilder = FactorGraphProtos.FactorGraph.newBuilder

  val weightBuilder = FactorGraphProtos.Weight.newBuilder

  val variableBuilder = FactorGraphProtos.Variable.newBuilder

  val factorBuilder = FactorGraphProtos.Factor.newBuilder

  val edgeBuilder = FactorGraphProtos.GraphEdge.newBuilder

  def addWeight(weightId: Long, isFixed: Boolean, initialValue: Double, desc: String) : Unit = {
    weightBuilder.setId(weightId)
    weightBuilder.setIsFixed(isFixed)
    if (isFixed) weightBuilder.setInitialValue(initialValue)
    weightBuilder.setDescription(desc)
    val obj = weightBuilder.build()
    obj.writeDelimitedTo(weightsOuput)
  }
  
  def addVariable(variableId: Long, isEvidence: Boolean, initialValue: Option[Double], 
    dataType: String, edgeCount: Long, cardinality: Option[Long]) : Unit = {

    variableBuilder.setId(variableId)
    variableBuilder.setEdgeCount(edgeCount)
    variableBuilder.setIsEvidence(isEvidence)
    initialValue.foreach(variableBuilder.setInitialValue)
    cardinality.foreach(variableBuilder.setCardinality)
    dataType match {
      case "Boolean" => 
        variableBuilder.setDataType(FactorGraphProtos.Variable.VariableDataType.BOOLEAN)
      case "Multinomial" =>
        variableBuilder.setDataType(FactorGraphProtos.Variable.VariableDataType.MULTINOMIAL)
        // variableBuilder.setCardinality(n)
    }    
    val obj = variableBuilder.build()
    obj.writeDelimitedTo(variablesOutput)
  }

  def addFactor(factorId: Long, weightId: Long, factorFunction: String, edgeCount: Long) : Unit = {
    
    factorBuilder.setId(factorId)
    factorBuilder.setWeightId(weightId)
    factorBuilder.setEdgeCount(edgeCount)
    val factorFunctionType = factorFunction match {
      case "ImplyFactorFunction" => FactorGraphProtos.Factor.FactorFunctionType.IMPLY
      case "OrFactorFunction" => FactorGraphProtos.Factor.FactorFunctionType.OR
      case "AndFactorFunction" => FactorGraphProtos.Factor.FactorFunctionType.AND
      case "EqualFactorFunction" => FactorGraphProtos.Factor.FactorFunctionType.EQUAL
      case "IsTrueFactorFunction" =>  FactorGraphProtos.Factor.FactorFunctionType.ISTRUE
    }
    factorBuilder.setFactorFunction(factorFunctionType)
    val obj = factorBuilder.build()
    obj.writeDelimitedTo(factorsOutput)
  }


  def addEdge(variableId: Long, factorId: Long, position: Long, isPositive: Boolean, 
    equalPredicate: Option[Long]) : Unit = {
    
    edgeBuilder.setVariableId(variableId)
    edgeBuilder.setFactorId(factorId)
    equalPredicate.foreach(edgeBuilder.setEqualPredicate)
    edgeBuilder.setPosition(position)
    edgeBuilder.setIsPositive(isPositive)
    val obj = edgeBuilder.build()
    obj.writeDelimitedTo(edgesOutput)
  }

  def writeMetadata(numWeights: Long, numVariables: Long, numFactors: Long, numEdges: Long,
    weightsFile: String, variablesFile: String, factorsFile: String, edgesFile: String) : Unit = {
      val graphDataBuilder = FactorGraphProtos.FactorGraph.newBuilder
      graphDataBuilder.setNumWeights(numWeights)
      graphDataBuilder.setNumVariables(numVariables)
      graphDataBuilder.setNumFactors(numFactors)
      graphDataBuilder.setNumEdges(numEdges)
      graphDataBuilder.setWeightsFile(weightsFile)
      graphDataBuilder.setVariablesFile(variablesFile)
      graphDataBuilder.setFactorsFile(factorsFile)
      graphDataBuilder.setEdgesFile(edgesFile)
      val obj = graphDataBuilder.build()
      obj.writeTo(metaDataOutput)
  }

  def close() : Unit = {
    weightsOuput.flush()
    variablesOutput.flush()
    factorsOutput.flush()
    edgesOutput.flush()
    metaDataOutput.flush()
  }

}