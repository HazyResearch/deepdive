package org.deepdive.inference

import org.deepdive.Logging
import org.deepdive.settings._
import org.deepdive.serialization.FactorGraphProtos
import java.io.OutputStream

class ProtobufSerializer(weightsOuput: OutputStream, variablesOutput: OutputStream, 
  factorsOutput: OutputStream, edgesOutput: OutputStream) extends Serializer with Logging {

  val factorGraphbuilder = FactorGraphProtos.FactorGraph.newBuilder

  def addWeight(weightId: Long, isFixed: Boolean, initialValue: Double, desc: String) : Unit = {
    val weightBuilder = FactorGraphProtos.Weight.newBuilder
    weightBuilder.setId(weightId)
    weightBuilder.setIsFixed(isFixed)
    if (isFixed) weightBuilder.setInitialValue(initialValue)
    weightBuilder.setDescription(desc)
    val obj = weightBuilder.build()
    weightsOuput.synchronized { obj.writeDelimitedTo(weightsOuput) } 
  }
  
  def addVariable(variableId: Long, initialValue: Option[Double], dataType: VariableDataType, 
    cardinality: Option[Long]) : Unit = {
    val variableBuilder = FactorGraphProtos.Variable.newBuilder
    variableBuilder.setId(variableId)
    if (initialValue.isDefined) variableBuilder.setInitialValue(initialValue.get)
    dataType match {
      case BooleanType => 
        variableBuilder.setDataType(FactorGraphProtos.Variable.VariableDataType.BOOLEAN)
      case MultinomialType(n) =>
        variableBuilder.setDataType(FactorGraphProtos.Variable.VariableDataType.MULTINOMIAL)
        variableBuilder.setCardinality(n)
    }    
    val obj = variableBuilder.build()
    variablesOutput.synchronized { obj.writeDelimitedTo(variablesOutput) }
  }

  def addFactor(factorId: Long, weightId: Long, factorFunction: String) : Unit = {
    val factorBuilder = FactorGraphProtos.Factor.newBuilder
    factorBuilder.setId(factorId)
    factorBuilder.setWeightId(weightId)
    val factorFunctionType = factorFunction match {
      case "ImplyFactorFunction" => FactorGraphProtos.Factor.FactorFunctionType.IMPLY
      case "OrFactorFunction" => FactorGraphProtos.Factor.FactorFunctionType.OR
      case "AndFactorFunction" => FactorGraphProtos.Factor.FactorFunctionType.AND
      case "EqualFactorFunction" => FactorGraphProtos.Factor.FactorFunctionType.EQUAL
      case "IsTrueFactorFunction" =>  FactorGraphProtos.Factor.FactorFunctionType.ISTRUE
    }
    factorBuilder.setFactorFunction(factorFunctionType)
    val obj = factorBuilder.build()
    factorsOutput.synchronized { obj.writeDelimitedTo(factorsOutput) }
  }


  def addEdge(variableId: Long, factorId: Long, position: Long, isPositive: Boolean) : Unit = {
    val edgeBuilder = FactorGraphProtos.GraphEdge.newBuilder
    edgeBuilder.setVariableId(variableId)
    edgeBuilder.setFactorId(factorId)
    edgeBuilder.setPosition(position)
    if (!isPositive) edgeBuilder.setIsPositive(isPositive)
    val obj = edgeBuilder.build()
    edgesOutput.synchronized { obj.writeDelimitedTo(edgesOutput) }
  }

  def close() : Unit = {
    weightsOuput.flush()
    variablesOutput.flush()
    factorsOutput.flush()
    edgesOutput.flush()
  }

}