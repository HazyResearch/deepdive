package org.deepdive.inference

import java.io.File
import org.deepdive.calibration._
import org.deepdive.Logging
import org.deepdive.settings.FactorFunctionVariable
import scala.collection.mutable.{Map => MMap}

trait MemoryInferenceDataStoreComponent extends InferenceDataStoreComponent{

  lazy val inferenceDataStore = new MemoryInferenceDataStore

  class MemoryInferenceDataStore extends InferenceDataStore with Logging {

    val variables = MMap[Long, Variable]()
    val factors = MMap[Long, Factor]()
    val weights = MMap[Long, Weight]()
    
    def init() = {
      variables.clear()
      factors.clear()
      weights.clear()
      log.info("initialized")
    }

    def getLocalVariableIds(rowMap: Map[String, Any], factorVar: FactorFunctionVariable) : Array[Long] = {
      rowMap(s"${factorVar.relation}.id") match {
        case x : Array[Long] => x
        case x : Long => Array(x)
        case _ => Array()
      }
    }

    def flush() = {
      log.info("flushing data")
    }

    def BatchSize = None

    def addFactor(factor: Factor) = {
      factors += Tuple2(factor.id, factor)
    }
    
    def addVariable(variable: Variable) = {
      variables += Tuple2(variable.id, variable)
    }
    
    def addWeight(weight: Weight) = {
      weights += Tuple2(weight.id, weight)
    }

    def dumpFactorGraph(variablesFile: File, factorsFile: File, weightsFile: File) = {
      throw new RuntimeException("not yet implemented")
    }

    def writebackInferenceResult(variableOutputFile: String) = {
      throw new RuntimeException("not yet implemented")
    }

    def getCalibrationData(variable: String, buckets: List[Bucket]) : Map[Bucket, BucketData] = {
      throw new RuntimeException("not yet implemented")
    }

  }
  
}