package org.deepdive.inference

import org.deepdive.settings._

trait Serializer {

  def addWeight(weightId: Long, isFixed: Boolean, initialValue: Double) : Unit
  def addVariable(variableId: Long, isEvidence: Boolean, initialValue: Double, 
    dataType: String, edgeCount: Long, cardinality: Long) : Unit
  def addFactor(factorId: Long, weightId: Long, factorFunction: String, edgeCount: Long) : Unit
  def addEdge(variableId: Long, factorId: Long, position: Long, isPositive: Boolean, 
    equalPredicate: Long) : Unit
  def writeMetadata(numWeights: Long, numVariables: Long, numFactors: Long, numEdges: Long,
    weightsFile: String, variablesFile: String, factorsFile: String, edgesFile: String) : Unit  
  def close() : Unit

}