package org.deepdive.inference

import org.deepdive.settings._

trait Serializer {

  def addWeight(weightId: Long, isFixed: Boolean, initialValue: Double, weightLength: Long) : Unit
  def addVariable(variableId: Long, isEvidence: Boolean, initialValue: Double) : Unit
  def addFactor(factorId: Long, factorFunction: String) : Unit
  def addEdge(variableId: Long, factorId: Long, weightId: Long, position: Long) : Unit
  def writeMetadata(numWeights: Long, numVariables: Long, numFactors: Long, numEdges: Long,
    weightsFile: String, variablesFile: String, factorsFile: String, edgesFile: String) : Unit  
  def close() : Unit

}