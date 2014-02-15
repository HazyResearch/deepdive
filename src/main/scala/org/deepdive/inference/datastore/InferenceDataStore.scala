package org.deepdive.inference

import org.deepdive.settings.FactorFunctionVariable
import org.deepdive.calibration._
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

    /* 
     * The number of tuples in each batch. If not defined, we use one large batch. 
     * The user can overwrite this number using the inference.batch_size config setting.
     */
    def BatchSize : Option[Int]

    /* Returns a list of variable IDs for all variables in the given factor function */
    def getLocalVariableIds(rowMap: Map[String, Any], factorVar: FactorFunctionVariable) : Array[Long]

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

    /* 
     * Dumps the factor graphs with the given serializier
     */
    def dumpFactorGraph(serializer: Serializer, file: File) : Unit

    /* 
     * Writes inference results produced by the sampler back to the data store.
     * The given file is a space-separated file with three columns:
     * VariableID, LastSampleValue, ExpectedValue
     */
    def writebackInferenceResult(variableSchema: Map[String, String],
        variableOutputFile: String, weightsOutputFile: String) : Unit

    
    /* 
     * Gets calibration data for the given buckets.
     * writebackInferenceResult must be called before this method can be called.
     */
    def getCalibrationData(variable: String, buckets: List[Bucket]) : Map[Bucket, BucketData]

  }
  
}