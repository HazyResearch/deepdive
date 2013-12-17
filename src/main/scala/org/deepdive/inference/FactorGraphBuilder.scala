package org.deepdive.inference

import anorm._
import org.deepdive.extraction.datastore._
import org.deepdive.settings._
import org.deepdive.context.Context
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

  val BATCH_SIZE = 10000
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
      addFactorsAndVariables(factorDesc, holdoutFraction)
      log.info(s"flushing data store")
      sender ! AddFactorsResult(factorDesc.name, true)
    case _ => 
  }


  def addFactorsAndVariables(factorDesc: FactorDesc, holdoutFraction: Double) {
    dataStore.queryAsMap(factorDesc.inputQuery).iterator.grouped(BATCH_SIZE).foreach { group =>
     group.foreach { case row =>
      addVariablesForRow(row, factorDesc, holdoutFraction)
      addFactorForRow(row, factorDesc)
     }
     log.debug(s"flushing batch_size=${group.size} for factor_name=${factorDesc.name}.")
     inferenceDataStore.flush()
    }
  }

  private def addVariablesForRow(rowMap: Map[String, Any], factorDesc: FactorDesc,
    holdoutFraction: Double) : Unit = {
    val variableColumns = factorDesc.func.variables.toList
    val variableLocalIds = variableColumns.map { varColumn =>
      rowMap(s"${varColumn.relation}.id").asInstanceOf[Long]
    }
    val variableValues = variableColumns.map { varColumn =>
      rowMap.get(varColumn.toString).get match {
        case Some(x) => Option(x)
        case None => None
        case x => Option(x)
      }
    }

    val newVariables = (variableColumns, variableLocalIds, 
      variableValues).zipped.foreach { case(varColumn, varId, varValue) => 

        // Flip a coin to check if the variable should part of the holdout
        val isEvidence = varValue.isDefined
        val isHoldout = isEvidence && (rng.nextDouble() < holdoutFraction)
        val isQuery = !isEvidence || (isEvidence && isHoldout)

        // Build the variable
        // TODO: Right now, all our variables are boolean. How do we support others?
        val varObj = Variable(variableIdCounter.getAndIncrement(), VariableDataType.Boolean, 
           0.0, !isQuery, isQuery)
        // Store the variable using a unique key
        val variableKey = VariableMappingKey(varColumn.headRelation, varId, varColumn.field)
        if (!inferenceDataStore.hasVariable(variableKey)) {
          // log.debug(s"added variable=${variableKey}")
          inferenceDataStore.addVariable(variableKey, varObj)
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
    val weightIdentifier = factorDesc.name + "_" + factorWeightValues.mkString
    // log.debug(s"added weight=${weightIdentifier}")

    // Add the weight to the factor store
    val weightId = inferenceDataStore.getWeightId(weightIdentifier).getOrElse(
      weightIdCounter.getAndIncrement().toLong)
    val weight = Weight(weightId, 0.0, factorDesc.weight.isInstanceOf[KnownFactorWeight])
    inferenceDataStore.addWeight(weightIdentifier, weight)

    // Build and Add Factor
    val newFactorId = factorIdCounter.getAndIncrement()
    val factorVariables = factorDesc.func.variables.zipWithIndex.map { case(factorVar, position) =>
      val localId = rowMap(s"${factorVar.relation}.id").asInstanceOf[Long]
      val variableKey = VariableMappingKey(factorVar.headRelation, localId, factorVar.field)
      val variableId = inferenceDataStore.getVariableId(variableKey).getOrElse {
        log.error(s"variable_key=${variableKey} not found.")
        throw new RuntimeException(s"variable_key=${variableKey} not found.")
      }
      FactorVariable(newFactorId.toLong, position, true, variableId)
    }
    val newFactor = Factor(newFactorId, factorDesc.func.getClass.getSimpleName, weight, 
      factorVariables.toList)
    inferenceDataStore.addFactor(newFactor)
  }


}