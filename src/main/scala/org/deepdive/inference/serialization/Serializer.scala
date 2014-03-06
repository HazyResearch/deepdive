package org.deepdive.inference

import org.deepdive.settings._

trait Serializer {

  def addWeight(weightId: Long, isFixed: Boolean, initialValue: Double, desc: String) : Unit
  def addVariable(variableId: Long, initialValue: Option[Double], 
    dataType: VariableDataType, edgeCount: Long, cardinality: Option[Long]) : Unit
  def addFactor(factorId: Long, weightId: Long, factorFunction: String, edgeCount: Long) : Unit
  def addEdge(variableId: Long, factorId: Long, position: Long, isPositive: Boolean) : Unit
  def close() : Unit

}