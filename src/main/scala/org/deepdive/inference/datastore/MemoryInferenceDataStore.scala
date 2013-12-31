package org.deepdive.inference

import au.com.bytecode.opencsv.{CSVWriter, CSVReader}
import java.io.{File, FileWriter, FileReader}
import org.deepdive.calibration._
import org.deepdive.Logging
import org.deepdive.settings.FactorFunctionVariable
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
      val weightsData = weights.values.iterator.map(_.asInstanceOf[CSVFormattable])
      writeCSV(weightsData, weightsFile)
      log.info(s"wrote weights to file=${weightsFile.getAbsolutePath}")

      val factorData = factors.values.iterator.map { factor =>
        new CSVFormattable {
          def toCSVRow = Array(factor.id.toString, factor.weightId.toString, 
            factor.factorFunction.toString)
        }
      }
      writeCSV(factorData, factorsFile)
      log.info(s"wrote factors to file=${factorsFile.getAbsolutePath}")


      val variablesData = factors.values.flatMap(_.variables).iterator.map { fVariable =>
        val variable = variables(fVariable.variableId)
        new CSVFormattable {
          def toCSVRow = Array(variable.id.toString, fVariable.factorId.toString,
            fVariable.position.toString, fVariable.positive.toString, 
            variable.dataType.toString, variable.initialValue.toString,
            variable.isEvidence.toString, variable.isQuery.toString)
        }
      }
      writeCSV(variablesData, variablesFile)
      log.info(s"wrote variables to file=${variablesFile.getAbsolutePath}")
    }

    def writebackInferenceResult(variableOutputFile: String) : Unit = {
      val reader = new CSVReader(new FileReader(variableOutputFile), '\t')
      val inferenceResult = reader.readAll()
      inferenceResult.foreach { case Array(variableId, lastSample, probability) => 
        variableValues += Tuple2(variableId.toLong, probability.toDouble)
      }
      reader.close()
      log.info(s"read inference result from file=${variableOutputFile}")
    }

    def getCalibrationData(variable: String, buckets: List[Bucket]) : Map[Bucket, BucketData] = {
      val Array(relation, column) = variable.split('.')
      val variablesWithValues = variables.values
        .filter(v => v.mappingRelation == relation && v.mappingColumn == column)
        .map (v => (v, variableValues.get(v.id).getOrElse(0.0)))
      buckets.map { bucket =>
        val relevantVars = variablesWithValues.filter { case(variable, variableValue) =>
          variableValue >= bucket.from && variableValue <= bucket.to
        }
        val numVariables = relevantVars.size
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