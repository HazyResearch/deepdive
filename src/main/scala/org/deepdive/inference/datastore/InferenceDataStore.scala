package org.deepdive.inference

import org.deepdive.settings.FactorFunctionVariable
import org.deepdive.calibration._
import org.deepdive.settings._
import java.io.File


trait InferenceDataStore {

    /* Initializes the data store. This method must be called before any other methods in this class. */
    def init() : Unit

    /* 
     * The number of tuples in each batch. If not defined, we use one large batch. 
     * The user can overwrite this number using the inference.batch_size config setting.
     */
    def BatchSize : Option[Int]

    /* Generate a grounded graph based on the factor description */
    def groundFactorGraph(schema: Map[String, _ <: VariableDataType],
        factorDescs: Seq[FactorDesc], calibrationSettings: CalibrationSettings, 
        skipLearning: Boolean, weightTable: String, dbSettings: DbSettings = null, parallelGrounding: Boolean) : Unit 

    /* 
     * Dumps the factor graphs with the given serializier
     */
    def dumpFactorGraph(serializer: Serializer, schema: Map[String, _ <: VariableDataType],
        factorDescs: Seq[FactorDesc], holdoutFraction: Double, holdoutQuery: Option[String],
        weightsPath: String, variablesPath: String, factorsPath: String, edgesPath: String,
        parallelGrounding: Boolean) : Unit

    /* 
     * Writes inference results produced by the sampler back to the data store.
     * The given file is a space-separated file with three columns:
     * VariableID, LastSampleValue, ExpectedValue
     */
    def writebackInferenceResult(variableSchema: Map[String, _ <: VariableDataType],
        //variableOutputFile: String, weightsOutputFile: String, parallelGrounding: Boolean) : Unit
        variableOutputFile: String, weightsOutputFile: String, parallelGrounding: Boolean, dbSettings: DbSettings) : Unit


    /* 
     * Gets calibration data for the given buckets.
     * writebackInferenceResult must be called before this method can be called.
     */
    def getCalibrationData(variable: String, dataType: VariableDataType, buckets: List[Bucket]) : Map[Bucket, BucketData]

}

/* Stores the factor graph and inference results. */
trait InferenceDataStoreComponent {

  def inferenceDataStore : InferenceDataStore

}