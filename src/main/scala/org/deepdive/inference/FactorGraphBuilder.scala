package org.deepdive.inference

import anorm._
import org.deepdive.datastore.Connected
import org.deepdive.settings._
import org.deepdive.context.Context
import akka.actor.{Actor, ActorRef, Props, ActorLogging}
import scala.collection.mutable.{Map, ArrayBuffer}
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.JavaConversions._

object FactorGraphBuilder {
  
  // Messages
  sealed trait Message
  case class AddFactorsAndVariables(factorDesc: FactorDesc) extends Message
  // case class AddVariables(relation: Relation, fields: Set[String]) extends Message
  case class AddFactorsResult(name: String, success: Boolean)
  // case class AddVariablesResult(name: String, success: Boolean)

  def props: Props = Props[FactorGraphBuilder]()

}

class FactorGraphBuilder extends Actor with Connected with ActorLogging {
  import FactorGraphBuilder._

  val factorStore = new PostgresFactorStore
  val factorFunctionIdCounter = new AtomicInteger
  val variableIdCounter = new AtomicInteger
  val factorIdCounter = new AtomicInteger
  val weightIdCounter = new AtomicInteger

  override def preStart() {
    log.debug("Starting")
    factorStore.init()
  }

  def receive = {
    case AddFactorsAndVariables(factorDesc) =>
      log.debug(s"Adding factors and variables for factor_name=${factorDesc.name}")
      addFactorsAndVariables(factorDesc)
      factorStore.flush()
      sender ! AddFactorsResult(factorDesc.name, true)
    case _ => 
  }


  def addFactorsAndVariables(factorDesc: FactorDesc) {

    SQL(factorDesc.inputQuery)().foreach { row =>
      addVariablesForRow(row, factorDesc)
      addFactorForRow(row, factorDesc)
    }

  }

  private def addVariablesForRow(row: SqlRow, factorDesc: FactorDesc) : Unit = {
    val rowMap = row.asMap.toMap

    val variableColumns = factorDesc.func.variables.toList
    val variableLocalIds = variableColumns.map { varColumn =>
      row[Long](s"${varColumn.relation}.id")
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
        // Build the variable
        // TODO: Right now, all our variables are boolean. How do we support others?
        val varObj = Variable(variableIdCounter.getAndIncrement(), VariableDataType.Boolean, 
           0.0, varValue.isDefined, !varValue.isDefined)
        // Store the variable using a unique key
        val variableKey = s"${varId}_${varColumn.key}"
        if (!factorStore.hasVariable(variableKey)) {
          // log.debug(s"added variable=${variableKey}")
          factorStore.addVariable(variableKey, varObj)
        }
      }
  }

  private def addFactorForRow(row: SqlRow, factorDesc: FactorDesc) : Unit = {
    
    val rowMap = row.asMap.toMap
    // Build and get or add the factorWeight
    val factorWeightValues = for {
      variableName <- factorDesc.weight.variables
      variableValue <- rowMap.get(variableName)
    } yield variableValue.asInstanceOf[Option[_]].getOrElse("")
    // A unique identifier for the weight based on the factor name
    val weightIdentifier = factorDesc.name + "_" + factorWeightValues.mkString
    // log.debug(s"added weight=${weightIdentifier}")

    // Add the weight to the factor store
    val weight = factorStore.getWeight(weightIdentifier) match {
      case Some(weight) => weight
      case None =>
        val newWeight = Weight(weightIdCounter.getAndIncrement(), 0.0, 
          factorDesc.weight.isInstanceOf[KnownFactorWeight])
        factorStore.addWeight(weightIdentifier, newWeight)
        newWeight
    }

    // Build and Add Factor
    val newFactorId = factorIdCounter.getAndIncrement()
    val factorVariables = factorDesc.func.variables.zipWithIndex.map { case(factorVar, position) =>
      val localId = row[Long](s"${factorVar.relation}.id")
      val variableKey = s"${localId}_${factorVar.key}"
      val variableId = factorStore.getVariableId(variableKey).getOrElse {
        log.error(s"variable_key=${variableKey} not found.")
        throw new RuntimeException(s"variable_key=${variableKey} not found.")
      }
      FactorVariable(newFactorId.toLong, position, true, variableId)
    }
    val newFactor = Factor(newFactorId, factorDesc.func.getClass.getSimpleName, weight, 
      factorVariables.toList)
    factorStore.addFactor(newFactor)
  }


}