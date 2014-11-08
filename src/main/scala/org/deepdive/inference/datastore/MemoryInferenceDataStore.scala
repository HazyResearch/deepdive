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

    def dumpFactorGraph(serializer: Serializer, schema: Map[String, _ <: VariableDataType],
      factorDescs: Seq[FactorDesc], holdoutFraction: Double, holdoutQuery: Option[String],
      weightsPath: String, variablesPath: String, factorsPath: String, edgesPath: String,
      parallelGrounding: Boolean) = {
      // Weights
      weights.values.foreach { w => serializer.addWeight(w.id, w.isFixed, w.value) }
      // variables.values.foreach { v =>  serializer.addVariable(v.id, v.initialValue.isDefined, 
      //   v.initialValue, v.dataType.toString, 0, v.dataType.cardinality) }
      variables.values.foreach { v =>  serializer.addVariable(v.id, v.isEvidence, 
        v.initialValue, v.dataType.toString, 0, v.dataType.cardinality) }
      factors.values.foreach { f => serializer.addFactor(f.id, f.weightId, f.factorFunction, 0) }
      factors.values.flatMap(_.variables).foreach { edge =>
        // serializer.addEdge(edge.variableId, edge.factorId, edge.position, edge.positive, None)
        serializer.addEdge(edge.variableId, edge.factorId, edge.position, edge.positive, 0)
      }
      serializer.close()
    }

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

    private def writeCSV(data: Iterator[CSVFormattable], file: File, sep : Char = '\t') {
      val fw = new FileWriter(file)
      val csv = new CSVWriter(fw, sep)
      data.foreach (obj => csv.writeNext(obj.toCSVRow))
      csv.close()
    }

  }
  
}