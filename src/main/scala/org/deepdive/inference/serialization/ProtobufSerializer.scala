package org.deepdive.inference

import org.deepdive.Logging
import org.deepdive.serialization.FactorGraphProtos

class ProtobufSerializer extends Serializer with Logging {

  val factorGraphbuilder = FactorGraphProtos.FactorGraph.newBuilder

  def addWeight(weightId: Long, isFixed: Boolean, initialValue: Double, desc: String) : Unit = {
    val weightBuilder = FactorGraphProtos.Weight.newBuilder
    weightBuilder.setId(weightId)
    weightBuilder.setIsFixed(isFixed)
    if (isFixed) weightBuilder.setInitialValue(initialValue)
    weightBuilder.setDescription(desc)
    factorGraphbuilder.addWeight(weightBuilder.build)
  }
  
  def addVariable(variableId: Long, initialValue: Option[Double], dataType: String) : Unit = {
    val variableBuilder = FactorGraphProtos.Variable.newBuilder
    variableBuilder.setId(variableId)
    if (initialValue.isDefined) variableBuilder.setInitialValue(initialValue.get)
    val variableDataType = dataType match {
      case "Boolean" => FactorGraphProtos.Variable.VariableDataType.BOOLEAN
    }
    variableBuilder.setDataType(variableDataType)
    factorGraphbuilder.addVariable(variableBuilder.build)
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
    factorGraphbuilder.addFactor(factorBuilder.build)
  }


  def addEdge(variableId: Long, factorId: Long, position: Long, isPositive: Boolean) : Unit = {
    val edgeBuilder = FactorGraphProtos.GraphEdge.newBuilder
    edgeBuilder.setVariableId(variableId)
    edgeBuilder.setFactorId(factorId)
    edgeBuilder.setPosition(position)
    if (!isPositive) edgeBuilder.setIsPositive(isPositive)
    factorGraphbuilder.addEdge(edgeBuilder.build)
  }

  def write(path: java.io.File) : Unit = {
    val output = new java.io.FileOutputStream(path)
    factorGraphbuilder.build().writeTo(output)
    output.close()
  }

}