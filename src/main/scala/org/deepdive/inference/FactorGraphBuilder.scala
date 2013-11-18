package org.deepdive.inference

import anorm._
import org.deepdive.datastore.Connected
import org.deepdive.context._
import org.deepdive.context.{Factor => FactorDescription, FactorFunction => FactorFunctionDesc}
import akka.actor.{Actor, ActorRef, Props, ActorLogging}
import scala.collection.mutable.{Map, ArrayBuffer}
import java.util.concurrent.atomic.AtomicInteger

object FactorGraphBuilder {
  
  // Messages
  case class AddFactorsForRelation(relation: Relation, factorDesc: FactorDescription)

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
    case AddFactorsForRelation(relation, factorDesc) =>
      log.debug(s"Adding variables and factors for ${relation.name}")
      addVariableAndFactorsForRelation(relation, factorDesc)
    case _ => 
  }

  def addVariableAndFactorsForRelation(relation: Relation, 
    factorDesc: FactorDescription) {

    // Create a new Factor Function
    val factorFunction = new FactorFunction(factorFunctionIdCounter.getAndIncrement(), factorDesc.name)

    // Select the primary key, all foreign keys, and all weight variables from the relation
    val selectFields = (Seq("id") ++ 
      relation.foreignKeys.map(_.childAttribute) ++ 
      factorDesc.weight.variables
    ).mkString(", ")
    SQL(s"SELECT $selectFields FROM ${relation.name}")().foreach { row =>
      
      // Build and add a new variable
      val localId = row[Long]("id")
      val globalId = variableIdCounter.getAndIncrement()
      // TODO: Set variable properties based on the variable type
      val newVariable = Variable(globalId, VariableType.CQS, 0.0, 1.0, 0.0)
      factorStore.addVariable(relation.name, localId, newVariable)
      
      // Build and get or add the factorWeight
      val factorWeightValues = for {
        variableName <- factorDesc.weight.variables
        variableType <- relation.schema.get(variableName)
        variableValue <- Some(buildWeightVariableValue(row, variableName, variableType))
      } yield variableValue
      val weightIdentifier = relation.name + "_" + factorWeightValues.mkString(",")
      val weight = factorStore.getWeight(weightIdentifier) match {
        case Some(weight) => weight
        case None =>
          val newWeight = Weight(weightIdCounter.getAndIncrement(), 0.0, 
            factorDesc.weight.isInstanceOf[KnownFactorWeight])
          factorStore.addWeight(weightIdentifier, newWeight)
          newWeight
      }

      // Build and Add Factor
      val factorVariables = factorDesc.func.variables.zipWithIndex.map { case(attributeName, position) =>
        for {
          foreignKey <- relation.foreignKeys.find(_.childAttribute == attributeName)
          variable <- factorStore.getVariable(foreignKey.parentRelation, row[Int](attributeName))
          factorVariable <- Some(FactorVariable(position, true, variable))
        } yield factorVariable
      }.flatten :+ FactorVariable(factorDesc.func.variables.size, true, newVariable)
      val newFactor = Factor(factorIdCounter.getAndIncrement(), factorFunction, weight, factorVariables.toList)
      factorStore.addFactor(newFactor)
    }
  }
  
  private def buildWeightVariableValue(row: SqlRow, variableName: String, variableType: String) = {
    variableType match {
      case "Long" => row[Long](variableName).toString
      case "String" => row[String](variableName)
      case "Text" => row[String](variableName)
      // TODO: Add More
      case _ => row[String](variableName)
    }
  }

  def writeToDatabase() {
    factorStore.flush()
  }

}