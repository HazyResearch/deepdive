package org.deepdive.inference

/* Stores the factor graph */
trait InferenceDataStoreComponent {

  def inferenceDataStore : InferenceDataStore

  trait InferenceDataStore {
    def init() : Unit
    def addFactor(factor: Factor) : Unit
    def addVariable(key: VariableMappingKey, variable: Variable) : Unit
    def hasVariable(key: VariableMappingKey) : Boolean 
    def getVariableId(key: VariableMappingKey) : Option[Long]
    def addWeight(identifier: String, weight: Weight)
    def getWeight(identifier: String) : Option[Weight]
    def flush() : Unit
  }
  
}