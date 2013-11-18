package org.deepdive.inference

import anorm._
import java.sql.Connection
import akka.event.Logging
import scala.collection.mutable.{Map, ArrayBuffer, Set}
import org.slf4j.LoggerFactory

class PostgresFactorStore(implicit val connection: Connection) {

  val log = LoggerFactory.getLogger(getClass);

  val factorFunctions = Set[FactorFunction]()
  val variables = Map[(String, Long), Variable]()
  val factors = ArrayBuffer[Factor]()
  val weights = Map[String, Weight]()

  // Prepares the data store to store the factor graph
  def init() {
    SQL("""drop table if exists variables; create table variables(id bigint primary key, variable_type varchar(4), 
      lower_bound decimal, uppper_bound decimal, initial_value decimal);""").execute()
    SQL("""drop table if exists factors; create table factors(id bigint primary key, weight_id bigint, 
      factor_function_id bigint);""").execute()
    SQL("""drop table if exists factor_variables; create table factor_variables(factor_id bigint, 
      variable_id bigint, position int, is_positive boolean);""").execute()
    SQL("""drop table if exists weights; create table weights(id bigint primary key, value decimal, is_fixed boolean);""").execute()
    SQL("""drop table if exists factor_functions; create table factor_functions(id bigint primary key, description varchar);""").execute()
  }

  // Add a new Factor to the datastore
  def addFactor(factor: Factor) {
    factors += factor
    factorFunctions += factor.factorFunction
  }

  // Add a new Variable to the datastore
  def addVariable(relationName: String, localId: Long, variable: Variable) {
    variables += Tuple2((relationName, localId), variable)
  }

  // Get a specific variable from the data store
  def getVariable(relationName: String, localId: Long) : Option[Variable] = {
    variables.get((relationName, localId))
  }

  // Get a specific weight form the data store
  def getWeight(identifier: String) = weights.get(identifier)

  // Add a weight to the data store
  def addWeight(identifier: String, weight: Weight) = { weights += Tuple2(identifier, weight) }

  // Flush out
  def flush(relationName: String) {
    
    // Insert Weights
    log.debug("Writing weights")
    val weightValues = weights.values.map { weight =>
      s"""(${weight.id}, ${weight.value}, ${weight.isFixed})"""
    }.mkString(", ")
    SQL(s"insert into weights(id, value, is_fixed) values $weightValues;").execute()
    // Clear the weights
    weights.clear()

    // Insert Factor Functions
    log.debug("Writing factor functions")
    val functionValues = factorFunctions.map { case func =>
      s"""(${func.id}, '${func.desc}')"""
    }.mkString(", ")
    SQL(s"insert into factor_functions(id, description) values $functionValues;").execute()
    factorFunctions.clear()

    // Insert Variables. TODO: Batch
    log.debug("Writing variables")
    variables.filterKeys(_._1 == relationName).values.foreach { variable =>
      SQL(s"""insert into variables(id, variable_type, lower_bound, uppper_bound, initial_value) VALUES
        (${variable.id}, '${variable.variableType}', ${variable.lowerBound}, ${variable.upperBound}, 
          ${variable.initialValue});""").execute()
    }
    // We keep the variables so that we can refer to them later on.
    // TODO: We need to make this more efficient. It's likely that the variables won't fit into memory.

    // Insert Factors. TODO: Batch
    log.debug("Writing factors")
    factors.foreach { factor => 
      SQL(s"""insert into factors(id, weight_id, factor_function_id) VALUES
        (${factor.id}, ${factor.weight.id}, ${factor.factorFunction.id});""").execute()
      factor.variables.foreach { factorVariable =>
        SQL(s"""insert into factor_variables(factor_id, variable_id, position, is_positive) VALUES
          (${factor.id}, ${factorVariable.value.id}, ${factorVariable.position}, ${factorVariable.positive});""").execute()
      }
    }
    factors.clear()
  }



}