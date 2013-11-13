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
  val factorWeights = ArrayBuffer[Weight]()
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
    val factorWeight = Weight(factorWeights.size, 0, true)
    factorWeights += factorWeight

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

  def writeToDatabase() {
    // TODO: Refactor this into somewhere else
    // Create the necessary tables if they do not yet exsit
    SQL("""drop table if exists variables; create table variables(id bigint primary key, variable_type varchar(4), 
      lower_bound decimal, uppper_bound decimal, initial_value decimal);""").execute()
    SQL("""drop table if exists factors; create table factors(id bigint primary key, weight_id bigint, 
      factor_function_id bigint);""").execute()
    SQL("""drop table if exists factor_variables; create table factor_variables(factor_id bigint, 
      variable_id bigint, position int, is_positive boolean);""").execute()
    SQL("""drop table if exists weights; create table weights(id bigint primary key, value decimal, is_fixed boolean);""").execute()
    SQL("""drop table if exists factor_functions; create table factor_functions(id bigint primary key, description varchar);""").execute()


    // TODO: We have to be smarter about bulk inserting
    // Unfortunately anorm doesn't support this, so this need to be done manually.

    // Insert Weights
    log.debug("Writing weights")
    val weightValues = factorWeights.map { weight =>
      s"""(${weight.id}, ${weight.value}, ${weight.isFixed})"""
    }.mkString(", ")
    SQL(s"insert into weights(id, value, is_fixed) values $weightValues;").execute()

    // Insert Factor Functions
    // TODO: Build function description!
    log.debug("Writing factor functions")
    val functionValues = factorFunctions.map { case(func, id) =>
      s"""($id, '')"""
    }.mkString(", ")
    SQL(s"insert into factor_functions(id, description) values $functionValues;").execute()

    // Insert Variables. TODO: Batch
    log.debug("Writing variables")
    variables.values.foreach { variable =>
      SQL(s"""insert into variables(id, variable_type, lower_bound, uppper_bound, initial_value) VALUES
        (${variable.id}, '${variable.variableType}', ${variable.lowerBound}, ${variable.upperBound}, 
          ${variable.initialValue});""").execute()
    }

    // Insert Factors. TODO: Batch
    log.debug("Writing factors")
    factors.foreach { factor => 
      SQL(s"""insert into factors(id, weight_id, factor_function_id) VALUES
        (${factor.id}, ${factor.weight.id}, ${factor.factorFunctionId});""").execute()
      factor.variables.foreach { factorVariable =>
        SQL(s"""insert into factor_variables(factor_id, variable_id, position, is_positive) VALUES
          (${factor.id}, ${factorVariable.value.id}, ${factorVariable.position}, ${factorVariable.positive});""").execute()
      }
    }

  }

}