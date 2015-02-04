package org.deepdive.inference

import au.com.bytecode.opencsv.{CSVWriter, CSVReader}
import java.io.{File, FileWriter, FileReader}
import org.deepdive.calibration._
import org.deepdive.Logging
import org.deepdive.settings._
import scala.collection.mutable.{Map => MMap}
import scala.collection.JavaConversions._

trait MemoryInferenceDataStoreComponent extends InferenceDataStoreComponent{

  lazy val inferenceDataStore = new MemoryInferenceDataStore

  class MemoryInferenceDataStore extends InferenceDataStore with Logging {

    val variables = MMap[Long, Variable]()
    val variableValues = MMap[Long, Double]()
    val factors = MMap[Long, Factor]()
    val weights = MMap[Long, Weight]()
    
    def init() = {
      variables.clear()
      factors.clear()
      weights.clear()
      log.info("initialized")
    }

    def groundFactorGraph(schema: Map[String, _ <: VariableDataType],
      factorDescs: Seq[FactorDesc], calibrationSettings: CalibrationSettings, 
      skipLearning: Boolean, weightTable: String, dbSettings: DbSettings, parallelGrounding: Boolean) : Unit = {

    }

    def BatchSize = None

    def writebackInferenceResult(variableSchema: Map[String, _ <: VariableDataType], 
      //variableOutputFile: String, weightsOutputFile: String, parallelGrounding: Boolean) : Unit = {
      variableOutputFile: String, weightsOutputFile: String, parallelGrounding: Boolean, dbSettings: DbSettings) : Unit = {
      // TODO
    }

    def getCalibrationData(variable: String, dataType: VariableDataType, buckets: List[Bucket]) : Map[Bucket, BucketData] = {
      // TODO
      return Map[Bucket, BucketData]()
      val Array(relation, column) = variable.split('.')
      val variablesWithValues = variables.values
        .filter(v => v.mappingRelation == relation && v.mappingColumn == column)
        .map (v => (v, variableValues.get(v.id).getOrElse(0.0)))
      buckets.map { bucket =>
        val relevantVars = variablesWithValues.filter { case(variable, variableValue) =>
          variableValue >= bucket.from && variableValue <= bucket.to
        }
        val numVariables = relevantVars.size
        // val numTrue = relevantVars.map(_._1).count(v => v.initialValue == Option(1.0) && v.isEvidence == true)
        // val numFalse = relevantVars.map(_._1).count(v => v.initialValue == Option(0.0) && v.isEvidence == true)
        val numTrue = relevantVars.map(_._1).count(v => v.initialValue == 1.0 && v.isEvidence == true)
        val numFalse = relevantVars.map(_._1).count(v => v.initialValue == 0.0 && v.isEvidence == true)
        
        (bucket, BucketData(numVariables, numTrue, numFalse))
      }.toMap
    }

  }
  
}