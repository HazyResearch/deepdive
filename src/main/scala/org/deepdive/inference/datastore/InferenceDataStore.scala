package org.deepdive.inference

import java.io.File

/* Stores the factor graph and inference results. */
trait InferenceDataStoreComponent {

  def inferenceDataStore : InferenceDataStore

  trait InferenceDataStore {

    /* Initializes the data store. This method must be called before any other methods in this class. */
    def init() : Unit

    /* 
     * Flushes the data store. This is called after each batch. 
     * If no batch size is defined, this method is called once for all tuples
     */
    def flush() : Unit

    /* The number of tuples in each batch. If not defined, we use one large batch. */
    def BatchSize : Option[Int]

    /* 
     * Add a new factor. 
     * IMPORTANT: This method may be called more than once for each factor. The implementation is
     * responsible for making sure that all factors are unique based on their id.
     */
    def addFactor(factor: Factor) : Unit
    
    /* 
     * Add a new variable. 
     * IMPORTANT: This method may be called more than once for each variable. The implementation is
     * responsible for making sure that all variables are unique based on their id.
     */
    def addVariable(variable: Variable) : Unit
    
    /* 
     * Add a new weight. 
     * IMPORTANT: This method may be called more than once for each weight. The implementation is
     * responsible for making sure that all weights are unique based on their id.
     */
    def addWeight(weight: Weight)

    /* Dumps the factor graphs to three files that will be read by the sampler.
     * Refer to the developer guide for the format of these files. 
     */
    def dumpFactorGraph(variablesFile: File, factorsFile: File, weightsFile: File) : Unit

    /* 
     * Writes inference results produced by the sampler  back to the data store.
     * The given file is a space-separated file with three columns:
     * VariableID, LastSample, ExpectedValue
     */
    def writebackInferenceResult(variableOutputFile: String) : Unit

  }
  
}