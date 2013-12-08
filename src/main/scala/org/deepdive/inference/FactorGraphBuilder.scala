package org.deepdive.inference

import anorm._
import org.deepdive.datastore.Connected
import org.deepdive.settings._
import akka.actor.{Actor, ActorRef, Props, ActorLogging}
import scala.collection.mutable.{Map, ArrayBuffer}
import java.util.concurrent.atomic.AtomicInteger

object FactorGraphBuilder {
  
  // Messages
  sealed trait Message
  case class AddFactorsForRelation(name: String, relation: Relation, factorDesc: Option[FactorDesc]) extends Message
  case class AddFactorsResult(name: String, success: Boolean)

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
    case AddFactorsForRelation(name, relation, factorDesc) =>
      log.debug(s"Adding variables and factors for relation=${relation}")
      addVariableAndFactorsForRelation(relation, factorDesc)
      writeToDatabase(relation.name)
      sender ! AddFactorsResult(name, true)
    case _ => 
  }

  def addVariableAndFactorsForRelation(relation: Relation, 
    factorDesc: Option[FactorDesc]) {

    // Select the primary key, all foreign keys, and all weight variables from the relation
    val selectFields = (Seq("id") ++ 
      relation.foreignKeys.map(_.childAttribute) ++ 
      factorDesc.map(_.weight.variables).getOrElse(Nil) ++
      relation.queryField.map(List(_)).getOrElse(Nil)
    ).mkString(", ")
    SQL(s"SELECT $selectFields FROM ${relation.name}")().foreach { row =>
      
      // Build and add a new variable
      val localId = row[Long]("id")
      val globalId = variableIdCounter.getAndIncrement()
      val evidenceValue = relation.queryField.map { field =>
        row.asMap.get(s"${relation.name}.${field}").orNull
      }.map {
        case None => null
        case null => null
        case Some(x) => x
        case x => x
      }.filter(_ != null)

      val queryFieldType = relation.queryField.map(x => relation.schema(x)).orNull
      val newVariable = Variable(globalId, VariableDataType.forAttributeType(queryFieldType), 
          0.0, evidenceValue.isDefined, !evidenceValue.isDefined)
      factorStore.addVariable(relation.name, localId, newVariable)
    
      // If a factor is associated with the relation add it to the data store.
      factorDesc.foreach { factorDesc => addFactorForRow(row, relation, factorDesc) }
      
    }
  }

  private def addFactorForRow(row: SqlRow, relation: Relation, factorDesc: FactorDesc) = {

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
      val newFactorId = factorIdCounter.getAndIncrement()
      val factorVariables = factorDesc.func.variables.zipWithIndex.map { case(attributeName, position) =>
        for {
          foreignKey <- relation.foreignKeys.find(_.childAttribute == attributeName)
          variable <- factorStore.getVariable(foreignKey.parentRelation, row[Long](attributeName))
          factorVariable <- Some(FactorVariable(newFactorId.toLong, position, true, variable))
        } yield factorVariable
      }.flatten
      val newFactor = Factor(newFactorId, factorDesc.func.getClass.getSimpleName, weight, 
        factorVariables.toList)
      factorStore.addFactor(newFactor)
  }

  private def buildWeightVariableValue(row: SqlRow, variableName: String, variableType: String) = {
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