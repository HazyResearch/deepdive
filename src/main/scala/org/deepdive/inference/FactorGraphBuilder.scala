package org.deepdive.inference

import anorm._
import org.deepdive.datastore.Connected
import org.deepdive.settings._
import org.deepdive.context.Context
import akka.actor.{Actor, ActorRef, Props, ActorLogging}
import scala.collection.mutable.{Map, ArrayBuffer}
import java.util.concurrent.atomic.AtomicInteger

object FactorGraphBuilder {
  
  // Messages
  sealed trait Message
  case class AddFactors(factorDesc: FactorDesc) extends Message
  case class AddVariables(relation: Relation, fields: Set[String]) extends Message
  case class AddFactorsResult(name: String, success: Boolean)
  case class AddVariablesResult(name: String, success: Boolean)

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
    case AddVariables(relation, fields) =>
      log.debug(s"Adding variables relation=${relation.name} and fields=${fields.mkString}")
      addVariables(relation, fields)
      writeToDatabase(relation.name)
      sender ! AddVariablesResult(relation.name, true)
    case AddFactors(factorDesc) =>
      log.debug(s"Adding factors for factor=${factorDesc.name}")
      val relation = Context.settings.findRelation(factorDesc.relation).getOrElse {
        log.error(s"Relation ${factorDesc.relation} not defined in schema")
        throw new RuntimeException(s"Relation ${factorDesc.relation} not defined in schema")
      }
      addFactors(factorDesc, relation)
      writeToDatabase(relation.name)
      sender ! AddFactorsResult(factorDesc.name, true)
    case _ => 
  }


  def addVariables(relation: Relation, fields: Set[String]) {
    val selectFields = (Seq("id") ++ fields).mkString(", ")
    
    SQL(s"SELECT $selectFields FROM ${relation.name}")().foreach { row =>
      val localId = row[Long]("id")
      // Create a new variable id for each variable
      val variableIds = fields.map { varName =>
        variableIdCounter.getAndIncrement()
      }
      // Get the values for the new variables/
      // If the cell has a value, use it as evidence
      val variableValues = fields.map { field =>
        row.asMap.get(s"${relation.name}.${field}").get match {
          case Some(x) => Option(x)
          case None => None
          case x => Option(x)
        }
      }
      // Build and add new variables
      val newVariables = (fields, variableIds, 
        variableValues).zipped.foreach { case(field, varId, varValue) => 
        // Find the data type of the variable field.
        val queryFieldType = relation.schema.get(field).getOrElse {
          log.error(s"Could not find ${field} in schema of ${relation.name}")
          throw new RuntimeException(s"Could not find ${field} in schema of ${relation.name}")
        }
        // Build the variable
        val varObj = Variable(varId, VariableDataType.forAttributeType(queryFieldType), 
           0.0, varValue.isDefined, !varValue.isDefined)
        // Store the variable using a unique key
        val variableKey = s"${localId}_${field}"
        factorStore.addVariable(relation.name, variableKey, varObj)
      }
    }
  }

  def addFactors(factorDesc: FactorDesc, relation: Relation) {
    // Select the primary key, all foreign keys, and all weight variables needed 
    // for the factor
    val selectFields = (Seq("id") ++ 
      factorDesc.func.variables.flatMap(_.foreignKey) ++
      factorDesc.weight.variables
    ).mkString(", ")

    SQL(s"SELECT $selectFields FROM ${factorDesc.relation}")().foreach { row =>
      addFactorForRow(row, relation, factorDesc)
    }
  }

  private def addFactorForRow(row: SqlRow, relation: Relation, 
    factorDesc: FactorDesc) : Unit = {

    // Build and get or add the factorWeight
    val factorWeightValues = for {
      variableName <- factorDesc.weight.variables
      variableType <- relation.schema.get(variableName)
      variableValue <- Some(buildVariableWeightIdentifier(row, variableName, variableType))
    } yield variableValue
    // A unique identifier for the weight based on the factor name
    val weightIdentifier = factorDesc.name + "_" + factorWeightValues.mkString

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
      val foreignKeyField = factorVar.foreignKey.getOrElse("id")
      val variableField = factorVar.field
      for {
        foreignKey <- relation.foreignKeys.find(_.childAttribute == foreignKeyField)
        variableKey = s"${row[Long](foreignKeyField)}_${variableField}"
        variableId <- factorStore.getVariableId(foreignKey.parentRelation, variableKey)
        factorVariable = FactorVariable(newFactorId.toLong, position, true, variableId)
      } yield factorVariable
    }.flatten
    val newFactor = Factor(newFactorId, factorDesc.func.getClass.getSimpleName, weight, 
      factorVariables.toList)
    factorStore.addFactor(newFactor)
  }

  // Builds a unique identifier for the variable weight based on its value
  private def buildVariableWeightIdentifier(row: SqlRow, variableName: String, variableType: String) = {
    variableType match {
      case "Long" => row[Long](variableName).toString
      case "Integer" => row[Long](variableName).toString
      case "String" => row[String](variableName)
      case "Text" => row[String](variableName)
      // TODO: Add More
      case _ => row[String](variableName)
    }
  }

  def writeToDatabase(relationName: String) {
    factorStore.flush(relationName)
  }

}