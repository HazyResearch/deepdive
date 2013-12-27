package org.deepdive.inference

import anorm._
import org.deepdive.extraction.datastore._
import org.deepdive.settings._
import org.deepdive.Context
import org.deepdive.profiling.Profiler
import akka.actor.{Actor, ActorRef, Props, ActorLogging}
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.JavaConversions._
import scala.util.Random

object FactorGraphBuilder {

  // Implementation of FactorGraphBuilder using postgres components
  // TODO: Refactor this
  class PostgresFactorGraphBuilder extends FactorGraphBuilder with 
    PostgresExtractionDataStoreComponent with PostgresInferenceDataStoreComponent

  def props: Props = Props[PostgresFactorGraphBuilder]()

  // Messages
  sealed trait Message
  case class AddFactorsAndVariables(factorDesc: FactorDesc, holdoutFraction: Double) extends Message
  // case class AddVariables(relation: Relation, fields: Set[String]) extends Message
  case class AddFactorsResult(name: String, success: Boolean)
  // case class AddVariablesResult(name: String, success: Boolean)
}



trait FactorGraphBuilder extends Actor with ActorLogging { 
  self: ExtractionDataStoreComponent with InferenceDataStoreComponent =>
  
  import FactorGraphBuilder._

  val BATCH_SIZE = 20000
  val rng = new Random(1337)
  val factorFunctionIdCounter = new AtomicInteger
  val variableIdCounter = new AtomicInteger
  val factorIdCounter = new AtomicInteger
  val weightIdCounter = new AtomicInteger

  val profiler = context.actorSelection("/user/profiler")

  override def preStart() {
    log.info("Starting")
    inferenceDataStore.init()
  }

  def receive = {
    case AddFactorsAndVariables(factorDesc, holdoutFraction) =>
      val startTime = System.currentTimeMillis
      log.info(s"Processing factor_name=${factorDesc.name} with holdout_faction=${holdoutFraction}")
      addFactorsAndVariables(factorDesc, holdoutFraction)
      val endTime = System.currentTimeMillis
      profiler ! Profiler.FactorAdded(factorDesc, startTime, endTime)
      sender ! AddFactorsResult(factorDesc.name, true)
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
          val varObj = Variable(varId, VariableDataType.withName(factorDesc.func.variableDataType), 
             evidenceValue, !isQuery, isQuery, varColumn.headRelation, varColumn.field)
          // Store the variable using a unique key
          if (!inferenceDataStore.hasVariable(varId)) {
            // log.debug(s"added variable=${variableKey}")
            inferenceDataStore.addVariable(varObj)
          }
        }
      }
  }

  private def addFactorForRow(rowMap: Map[String, Any], factorDesc: FactorDesc) : Unit = {
    // Build and get or add the factorWeight
    val factorWeightValues = for {
      variableName <- factorDesc.weight.variables
      variableValue <- rowMap.get(variableName)
    } yield variableValue.asInstanceOf[Option[_]].getOrElse("")
    // A unique identifier for the weight based on the factor name
    val weightIdentifier = factorDesc.weightPrefix + "_" + factorWeightValues.mkString
    // log.debug(s"added weight=${weightIdentifier}")

    // Add the weight to the factor store
    val weightId = inferenceDataStore.getWeightId(weightIdentifier).getOrElse(
      weightIdCounter.getAndIncrement().toLong)
    val weightValue = factorDesc.weight match { 
      case x : KnownFactorWeight => x.value
      case _ => 0.0
    }
    val weight = Weight(weightId, weightValue, factorDesc.weight.isInstanceOf[KnownFactorWeight])
    inferenceDataStore.addWeight(weightIdentifier, weight)

    // Build and Add Factor
    val newFactorId = factorIdCounter.getAndIncrement()
    // TODO: Really ugly. Make this functional. I was in a hurry :)
    var positionCounter = -1
    val factorVariables = factorDesc.func.variables.zipWithIndex.flatMap { case(factorVar, position) =>
      val localIds = getLocalVariableIds(rowMap, factorVar)
      localIds.zipWithIndex.map { case(localId, index) =>
        positionCounter += 1
        FactorVariable(newFactorId.toLong, positionCounter, !factorVar.isNegated, localId)
      } 
    }
    val newFactor = Factor(newFactorId, factorDesc.func.getClass.getSimpleName, weight, 
      factorVariables.toList)
    inferenceDataStore.addFactor(newFactor)
  }


}