package org.deepdive.inference

import anorm._
import java.sql.Connection
import akka.event.Logging
import scala.collection.mutable.{Map, ArrayBuffer, Set}
import org.slf4j.LoggerFactory
import org.deepdive.context.Context
import org.deepdive.datastore.Utils.AnormSeq

class PostgresFactorStore(implicit val connection: Connection) {

  val BATCH_SIZE = 1000

  val log = Logging.getLogger(Context.system, this)

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
    SQL("""drop table if exists factor_functions; create table factor_functions(id bigint primary key, description text);""").execute()
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
    log.debug(s"Storing num=${weights.size} weights")
    writeWeights(weights.values)
    weights.clear()

    // Insert Factor Functions
    log.debug(s"Storing num=${factorFunctions.size} factor functions")
    writeFactorFunctions(factorFunctions)
    factorFunctions.clear()

    // Insert Variables
    val relationVariables = variables.filterKeys(_._1 == relationName).values
    log.debug(s"Storing num=${relationVariables.size} variables")
    writeVariables(relationVariables)

    // Insert Factors 
    log.debug(s"Storing ${factors.size} factors")
    writeFactors(factors)

    // Insert Factor Variables
    val factorVariables = factors.flatMap(_.variables)
    log.debug(s"Storing ${factorVariables.size} factor variables")
    writeFactorVariables(factorVariables)
    factors.clear()
    
  }

  private def writeWeights(values: Iterable[Weight]) {
    val sqlStatement = SQL("insert into weights(id, value, is_fixed) values ({id}, {value}, {is_fixed})")
    writeBatch(sqlStatement, values.map(Weight.toAnormSeq))
  }

  private def writeFactorFunctions(values: Iterable[FactorFunction]) {
    val sqlStatement = SQL("insert into factor_functions(id, description) values ({id}, {description})")
    writeBatch(sqlStatement, values.map(FactorFunction.toAnormSeq))
  }

  private def writeVariables(values: Iterable[Variable]) {
    val sqlStatement = SQL("""insert into variables(id, variable_type, lower_bound, uppper_bound, initial_value) 
      values ({id}, {variable_type}, {lower_bound}, {uppper_bound}, {initial_value})""")
    writeBatch(sqlStatement, values.map(Variable.toAnormSeq))
  }

  private def writeFactors(values: Iterable[Factor]) {
    val sqlStatement = SQL("""insert into factors (id, weight_id, factor_function_id)
      values ({id}, {weight_id}, {factor_function_id})""")
    writeBatch(sqlStatement, values.map(Factor.toAnormSeq))
  }

  private def writeFactorVariables(values: Iterable[FactorVariable]) {
    val sqlStatement = SQL("""insert into factor_variables(factor_id, variable_id, position, is_positive)
      values ({factor_id}, {variable_id}, {position}, {is_positive})""");
    writeBatch(sqlStatement, values.map(FactorVariable.toAnormSeq))
  }


  private def writeBatch(sqlStatement: SqlQuery, values: Iterable[AnormSeq]) {
    values.grouped(BATCH_SIZE).zipWithIndex.foreach { case(batch, i) =>
      log.debug(s"${BATCH_SIZE * i}/${values.size}")
      val batchInsert = new BatchSql(sqlStatement, batch.toSeq)
      batchInsert.execute()
    }
    log.debug(s"${values.size}/${values.size}")
  }



}