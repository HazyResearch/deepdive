package org.deepdive.inference

import java.io.File

/* Stores the factor graph */
trait InferenceDataStoreComponent {

  def inferenceDataStore : InferenceDataStore

  trait InferenceDataStore {

    /* Initializes the data store */
    def init() : Unit

    /* Flushes the data store. This is called after each batch if the batch size is defined */
    def flush() : Unit

    // Data is flushed automatically after each batch
    def BatchSize : Option[Int]
    
    def addFactor(factor: Factor) : Unit
    def addVariable(key: VariableMappingKey, variable: Variable) : Unit
    def hasVariable(key: VariableMappingKey) : Boolean 
    def getVariableId(key: VariableMappingKey) : Option[Long]
    def addWeight(identifier: String, weight: Weight)
    def getWeightId(identifier: String) : Option[Long]
    
    /* Dumps the factor graphs to three files */
    def dumpFactorGraph(factorMapFile: File, factorsFile: File, weightsFile: File) : Unit

    /* Writes sampling results back to the dataStore */
    def writeInferenceResult(file: String) : Unit

  }
  
}