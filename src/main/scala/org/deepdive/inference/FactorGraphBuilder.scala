package org.deepdive.inference

import anorm._
import org.deepdive.extraction.datastore._
import org.deepdive.settings._
import org.deepdive.Context
import akka.actor.{Actor, ActorRef, Props, ActorLogging}
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.JavaConversions._
import scala.util.{Random, Try, Success, Failure}

object FactorGraphBuilder {

  // Implementation of FactorGraphBuilder using postgres components
  // TODO: Refactor this
  class PostgresFactorGraphBuilder(val variableSchema: Map[String, String]) 
    extends FactorGraphBuilder with PostgresExtractionDataStoreComponent 
    with PostgresInferenceDataStoreComponent

  def props(variableSchema: Map[String, String]): Props = 
    Props(classOf[PostgresFactorGraphBuilder], variableSchema)

  // Messages
  sealed trait Message
  case class AddFactorsAndVariables(factorDesc: FactorDesc, holdoutFraction: Double) extends Message
  // case class AddVariables(relation: Relation, fields: Set[String]) extends Message
  case class AddFactorsResult(name: String, success: Boolean)
  // case class AddVariablesResult(name: String, success: Boolean)
}

trait FactorGraphBuilder extends Actor with ActorLogging { 
  self: ExtractionDataStoreComponent with InferenceDataStoreComponent =>
  
  def variableSchema: Map[String, String]

  val variableOffsetMap = variableSchema.keys.toList.sorted.zipWithIndex.toMap

  import FactorGraphBuilder._

  val rng = new Random(1337)
  val factorFunctionIdCounter = new AtomicInteger
  val variableIdCounter = new AtomicInteger
  val factorIdCounter = new AtomicInteger
  val weightIdCounter = new AtomicInteger

  override def preStart() {
    log.info("Starting")
    inferenceDataStore.init()
  }

  def receive = {
    case AddFactorsAndVariables(factorDesc, holdoutFraction) =>
      log.info(s"Processing factor_name=${factorDesc.name} with holdout_faction=${holdoutFraction}")
      // TODO: Failure handling
      addFactorsAndVariables(factorDesc, holdoutFraction)
      sender ! Success()
    case _ => 
  }


  def addFactorsAndVariables(factorDesc: FactorDesc, holdoutFraction: Double) {

    // If the underlying data store defines a batch size we use that.
    // If not, we process one big batch
    val batchIterator = inferenceDataStore.BatchSize match {
      case Some(x) => dataStore.queryAsMap(factorDesc.inputQuery).iterator.grouped(x)
      case None => Iterator(dataStore.queryAsMap(factorDesc.inputQuery))
    }

    batchIterator.foreach { group =>
     group.foreach { case row =>
      addVariablesForRow(row, factorDesc, holdoutFraction)
      addFactorForRow(row, factorDesc)
     }
     log.debug(s"flushing data for factor_name=${factorDesc.name}.")
     inferenceDataStore.flush()
    }
  }

  private def getLocalVariableIds(rowMap: Map[String, Any], 
    factorVar: FactorFunctionVariable) : Array[Long] = {
    if (factorVar.isArray)
      // TODO: Decouple this from the underlying datastore
      rowMap(s".${factorVar.relation}.id").asInstanceOf[Array[Long]]
    else
      Array(rowMap(s"${factorVar.relation}.id").asInstanceOf[Long])
  }

  private def addVariablesForRow(rowMap: Map[String, Any], factorDesc: FactorDesc,
    holdoutFraction: Double) : Unit = {
    val variableColumns = factorDesc.func.variables.toList
    val variableLocalIds = variableColumns.map { varColumn =>
      getLocalVariableIds(rowMap, varColumn)
    }
    val variableValues = variableColumns.map { varColumn =>
      rowMap.get(varColumn.toString).get match {
        case Some(x) => Option(x)
        case None => None
        case x => Option(x)
      }
    }

    val newVariables = (variableColumns, variableLocalIds, 
      variableValues).zipped.foreach { case(varColumn, varIds, varValue) => 

        // Flip a coin to check if the variable should part of the holdout
        val isEvidence = varValue.isDefined
        val isHoldout = isEvidence && (rng.nextDouble() < holdoutFraction)
        val isQuery = !isEvidence || (isEvidence && isHoldout)

        val evidenceValue = varValue match {
          case Some(x: Boolean) => if (x) 1.0 else 0.0
          case _ => 0.0
        }

        // Build the variable, one for each ID
        for (varId <- varIds) {
          val globalVariableId = getVariableId(varId, varColumn.key)
          val varObj = Variable(globalVariableId, VariableDataType.withName(factorDesc.func.variableDataType), 
             evidenceValue, !isQuery, isQuery, varColumn.headRelation, varColumn.field)
          // Store the variable using a unique key
          if (!inferenceDataStore.hasVariable(globalVariableId)) {
            // log.debug(s"added variable=${variableKey}")
            inferenceDataStore.addVariable(varObj)
          }
        }
      }
  }

  private def addFactorForRow(rowMap: Map[String, Any], factorDesc: FactorDesc) : Unit = {
    // Build and get or add the factorWeight
    val factorWeightValues = factorDesc.weight.variables.map { key => 
      rowMap.get(key).map(_.toString).getOrElse {
        val errorMsg = s"Could not find key=${key}. Available keys: ${rowMap.keySet.mkString(", ")}"
        log.error(errorMsg)
        throw new RuntimeException(errorMsg)
      }
    }

    val weightIdentifier = factorDesc.weightPrefix + "_" + factorWeightValues.mkString
    val valueAssignments =  factorDesc.weight.variables
      .zip(factorWeightValues)
      .map(_.productIterator.mkString("="))
      .mkString(",")
    val weightDescription = s"${factorDesc.name}(${valueAssignments})"

    // Add the weight to the factor store
    val weightId = inferenceDataStore.getWeightId(weightIdentifier).getOrElse(
      weightIdCounter.getAndIncrement().toLong)
    val weightValue = factorDesc.weight match { 
      case x : KnownFactorWeight => x.value
      case _ => 0.0
    }
    val weight = Weight(weightId, weightValue, 
      factorDesc.weight.isInstanceOf[KnownFactorWeight], weightDescription)
    inferenceDataStore.addWeight(weightIdentifier, weight)

    // Build and Add Factor
    val newFactorId = factorIdCounter.getAndIncrement()
    // TODO: Really ugly. Make this functional. I was in a hurry :)
    var positionCounter = -1
    val factorVariables = factorDesc.func.variables.zipWithIndex.flatMap { case(factorVar, position) =>
      val localIds = getLocalVariableIds(rowMap, factorVar)
      localIds.zipWithIndex.map { case(localId, index) =>
        positionCounter += 1
        val globalId = getVariableId(localId, factorVar.key)
        FactorVariable(newFactorId.toLong, positionCounter, !factorVar.isNegated, globalId)
      } 
    }
    val newFactor = Factor(newFactorId, factorDesc.func.getClass.getSimpleName, weight, 
      factorVariables.toList)
    inferenceDataStore.addFactor(newFactor)
  }

  private def getVariableId(localId: Long, key: String) : Long = {
    (localId * variableOffsetMap.size) + variableOffsetMap.get(key).getOrElse {
      val errorMsg = s"${key} not found in variable definitions. " + 
        s"Available variables: ${variableOffsetMap.keySet.mkString(",")}"
      throw new RuntimeException(errorMsg)
    }
  }


}