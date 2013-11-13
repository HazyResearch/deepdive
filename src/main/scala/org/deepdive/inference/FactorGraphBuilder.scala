package org.deepdive.inference

import anorm._
import org.deepdive.datastore.Connected
import org.deepdive.FactorFunction
import org.deepdive.context.{Relation, Factor => FactorDescription}
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

  val variableIdCounter = new AtomicInteger()
  val factorIdCounter = new AtomicInteger()

  val factorFunctions = Map[FactorFunction, Int]()
  val variables = Map[(String, Long), Variable]()
  val factors = ArrayBuffer[Factor]()

  override def preStart() {
    log.debug("Starting")
  }

  def receive = {
    case AddFactorsForRelation(relation, factorDesc) =>
      log.debug(s"Adding variables and factors for ${relation.name}")
      addVariableAndFactorsForRelation(relation, factorDesc)
    case _ => 
  }

  def addVariableAndFactorsForRelation(relation: Relation, 
    factorDesc: FactorDescription) {
    // Add the new factor function and weight
    val factorFuncId = factorFunctions.getOrElseUpdate(factorDesc.func, factorFunctions.size)
    // TODO: Parse the weight
    val factorWeight = Weight(0, 0, true)

    // Select the primary key and all foreign keys from the relation
    val selectFields = (Seq("id") ++ relation.foreignKeys.map(_.childAttribute)).mkString(", ")
    SQL(s"SELECT $selectFields FROM ${relation.name}")().foreach { row =>
      // Add Variable
      val localId = row[Long]("id")
      val globalId = variableIdCounter.getAndIncrement()
      val newVariable = Variable(globalId, VariableType.CQS, 0.0, 1.0, 0.0)
      variables += ((relation.name, localId) -> newVariable)
      // Add Factor
      val factorVariables = factorDesc.func.variables.zipWithIndex.map { case(attributeName, position) =>
        val foreignKey = relation.foreignKeys.find(_.childAttribute == attributeName).orNull
        val variable = variables.get(foreignKey.parentRelation, row[Int](attributeName)).orNull
        FactorVariable(position, true, variable)
      } :+ FactorVariable(factorDesc.func.variables.size, true, newVariable)
      val newFactor = Factor(factorIdCounter.getAndIncrement(), factorFuncId, factorWeight, factorVariables.toList)
      factors += newFactor
    }
  }

}