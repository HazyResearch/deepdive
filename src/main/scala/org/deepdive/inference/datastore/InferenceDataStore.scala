package org.deepdive.inference

import org.deepdive.settings.FactorFunctionVariable
import org.deepdive.calibration._
import org.deepdive.settings.FactorDesc
import java.io.File


/* Stores the factor graph and inference results. */
trait InferenceDataStoreComponent {

  def inferenceDataStore : InferenceDataStore

  trait InferenceDataStore {

    /* Initializes the data store. This method must be called before any other methods in this class. */
    def init() : Unit

    /* 
     * The number of tuples in each batch. If not defined, we use one large batch. 
     * The user can overwrite this number using the inference.batch_size config setting.
     */
    def BatchSize : Option[Int]

    /* Generate a grounded graph based on the factor description */
    def groundFactorGraph(factorDesc: FactorDesc, holdoutFraction: Double) : Unit 


    /* 
     * Dumps the factor graphs with the given serializier
     */
    def dumpFactorGraph(serializer: Serializer) : Unit

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