package org.deepdive.inference

import anorm._
import org.deepdive.datastore.PostgresDataStore
import org.deepdive.datastore.Utils.AnormSeq
import org.deepdive.Logging
import scala.collection.mutable.{Map, ArrayBuffer, Set}

/* Stores the factor graph */
trait PostgresInferenceDataStoreComponent extends InferenceDataStoreComponent {

  lazy val inferenceDataStore = new PostgresInferenceDataStore

  class PostgresInferenceDataStore extends InferenceDataStore with Logging {

    implicit lazy val connection = PostgresDataStore.borrowConnection()

    val BATCH_SIZE = 5000

    // val factorFunctions = Map[String, FactorFunction]()
    val variables = Map[VariableMappingKey, Variable]()
    val factors = ArrayBuffer[Factor]()
    val weights = Map[String, Weight]()
    val variableIdMap = Map[VariableMappingKey, Long]()

    def init() : Unit = {
      // weights(id, initial_value, is_fixed)
      SQL("""drop table if exists weights; 
        create table weights(id bigint primary key, 
        initial_value double precision, is_fixed boolean);""").execute()
      
      // factors(id, weight_id, factor_function)
      SQL("""drop table if exists factors; 
        create table factors(id bigint primary key, 
        weight_id bigint, factor_function text);""").execute()

      // variables(id, data_type, initial_value, is_evidence, is_query, mapping_relation, mapping_column, mapping_id)
      SQL("""drop table if exists variables; 
        create table variables(id bigint primary key, data_type text,
        initial_value double precision, is_evidence boolean, is_query boolean,
        mapping_relation text, mapping_column text, mapping_id integer);""").execute()
      
      // factor_variables(factor_id, variable_id, position, is_positive)
      SQL("""drop table if exists factor_variables; 
        create table factor_variables(factor_id bigint, variable_id bigint, 
        position int, is_positive boolean);""").execute()
    }

    def addFactor(factor: Factor) : Unit = {
      factors += factor
    }

    def addVariable(key: VariableMappingKey, variable: Variable) : Unit = {
      variables += Tuple2(key, variable)
    }

    def hasVariable(key: VariableMappingKey) : Boolean = {
      getVariableId(key).isDefined
    }

    def getVariableId(key: VariableMappingKey) : Option[Long] = {
      (variableIdMap ++ variables.mapValues(_.id)).get(key)
    }

    def getWeight(identifier: String) : Option[Weight] = weights.get(identifier)

    def addWeight(identifier: String, weight: Weight) = { weights += Tuple2(identifier, weight) }

    def flush() : Unit = {
      
      // Insert Weights
      log.debug(s"Storing num=${weights.size} relation=weights")
      writeWeights(weights.values)
      weights.clear()

      // Insert Factors 
      log.debug(s"Storing num=${factors.size} relation=factors")
      writeFactors(factors)

      // Insert Variables
      val variablesToFlush = variables
      val numEvidenceVariables = variablesToFlush.values.count(_.isEvidence)
      val numQueryVariables = variablesToFlush.values.count(_.isQuery)
      log.debug(s"Storing num=${variablesToFlush.size} num_evidence=${numEvidenceVariables} " +
        s"num_query=${numQueryVariables} relation=variables")
      writeVariables(variablesToFlush)
      // Add to the variable Id map, then clear
      variableIdMap ++= variablesToFlush.mapValues(_.id)
      variables --= variablesToFlush.keys


      // Insert Factor Variables
      val factorVariables = factors.flatMap(_.variables)
      log.debug(s"Storing num=${factorVariables.size} relation=factor_variables")
      writeFactorVariables(factorVariables)
      factors.clear()
      
    }

    private def writeWeights(values: Iterable[Weight]) {
      val sqlStatement = SQL("""insert into weights(id, initial_value, is_fixed) 
        values ({id}, {initial_value}, {is_fixed})""")
      writeBatch(sqlStatement, values.iterator.map(Weight.toAnormSeq(_)))
    }

    private def writeFactors(values: Iterable[Factor]) {
      val sqlStatement = SQL("""insert into factors (id, weight_id, factor_function)
        values ({id}, {weight_id}, {factor_function})""")
      writeBatch(sqlStatement, values.iterator.map(Factor.toAnormSeq))
    }

    private def writeVariables(values: Iterable[(VariableMappingKey, Variable)]) {
      val sqlStatement = SQL("""insert into variables(id, data_type, initial_value, is_evidence, is_query,
        mapping_relation, mapping_id, mapping_column) values ({id}, {data_type}, {initial_value}, {is_evidence}, 
        {is_query}, {mapping_relation}, {mapping_id}, {mapping_column})""")
      writeBatch(sqlStatement, values.iterator.map { case(key, variable) =>
        Variable.toAnormSeq(variable) ++ VariableMappingKey.toAnormSeq(key)
      })
    }

    private def writeFactorVariables(values: Iterable[FactorVariable]) {
      val sqlStatement = SQL("""insert into factor_variables(factor_id, variable_id, position, is_positive)
        values ({factor_id}, {variable_id}, {position}, {is_positive})""");
      writeBatch(sqlStatement, values.iterator.map(FactorVariable.toAnormSeq))
    }

    private def writeBatch(sqlStatement: SqlQuery, values: Iterator[AnormSeq]) {
      values.grouped(BATCH_SIZE).zipWithIndex.foreach { case(batch, i) =>
        val batchInsert = new BatchSql(sqlStatement, batch.toSeq)
        batchInsert.execute()
      }
    }

  }
}