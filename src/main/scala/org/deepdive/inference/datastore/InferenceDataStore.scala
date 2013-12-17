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
    def getWeightId(identifier: String) : Option[Long]
    
    def writeInferenceResult(file: String) : Unit

    def flush() : Unit
  }
  
}