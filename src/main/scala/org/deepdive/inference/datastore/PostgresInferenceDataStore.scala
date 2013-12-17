package org.deepdive.inference

import anorm._
import org.deepdive.datastore.PostgresDataStore
import org.deepdive.datastore.Utils.AnormSeq
import org.deepdive.Logging
import scala.collection.mutable.{Map, ArrayBuffer, Set}
import scala.io.Source
import java.io.{ByteArrayInputStream, StringWriter}
import au.com.bytecode.opencsv.CSVWriter

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
    val weightIdMap = Map[String, Long]()

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
      SQL("CREATE INDEX ON variables (mapping_id) using hash;")
      
      // factor_variables(factor_id, variable_id, position, is_positive)
      SQL("""drop table if exists factor_variables; 
        create table factor_variables(factor_id bigint, variable_id bigint, 
        position int, is_positive boolean);""").execute()
      SQL("CREATE INDEX ON factor_variables (factor_id) using hash;")
      SQL("CREATE INDEX ON factor_variables (variable_id) using hash;")

      // inference_result(id, last_sample, probability)
      SQL("""drop table if exists inference_result; 
        create table inference_result(id bigint primary key, last_sample boolean, 
        probability double precision);""").execute()
      SQL("CREATE INDEX ON inference_result (probability) using btree;")

      // A view for the mapped inference result
      SQL("""drop view if exists mapped_inference_result; 
      CREATE VIEW mapped_inference_result AS SELECT variables.*, inference_result.last_sample, inference_result.probability 
      FROM variables INNER JOIN inference_result ON variables.id = inference_result.id;
      """).execute()
    }

    def addFactor(factor: Factor) : Unit = {
      factors += factor
    }

    def addVariable(key: VariableMappingKey, variable: Variable) : Unit = {
      variables += Tuple2(key, variable)
      variableIdMap += Tuple2(key, variable.id)
    }

    def hasVariable(key: VariableMappingKey) : Boolean = {
      variableIdMap.contains(key)
    }

    def getVariableId(key: VariableMappingKey) : Option[Long] = {
      variableIdMap.get(key)
    }

    def getWeightId(identifier: String) : Option[Long] = weightIdMap.get(identifier)

    def addWeight(identifier: String, weight: Weight) = { 
      if (!weightIdMap.contains(identifier)) {
        weights += Tuple2(identifier, weight)
        weightIdMap += Tuple2(identifier, weight.id)
      }
    }

    def writeInferenceResult(file: String) : Unit = {
      val sqlStatement = SQL("""insert into inference_result(id, last_sample, probability)
        values ({id}, {last_sample}, {probability})""")
      Source.fromFile(file).getLines.grouped(BATCH_SIZE).foreach { values =>
        writeBatch(sqlStatement, values.map { row =>
          val Array(id, last_sample, probability) = row.split('\t')
          Seq(
            ("id", toParameterValue(id.toLong)), 
            ("last_sample", toParameterValue(last_sample.toBoolean)),
            ("probability", toParameterValue(probability.toDouble))
          )
        }.iterator)
      }
      // Generate a view for each (relation, column) combination.
      val relationsColumns = 
        SQL("SELECT DISTINCT mapping_relation, mapping_column from variables;")().map { row =>
        Tuple2(row[String]("mapping_relation"), row[String]("mapping_column"))
      }.toSet

      relationsColumns.foreach { case(relationName, columnName) => 
        val view_name = s"${relationName}_${columnName}_inference"
        log.info(s"creating view ${view_name}")
        SQL(s"""DROP VIEW IF EXISTS ${view_name}; CREATE VIEW ${view_name} AS
          SELECT ${relationName}.*, mir.last_sample, mir.probability FROM
          ${relationName} JOIN
            (SELECT mir.last_sample, mir.probability, mir.mapping_id 
            FROM mapped_inference_result mir 
            WHERE mapping_relation = '${relationName}' AND mapping_column = '${columnName}') 
          mir ON ${relationName}.id = mir.mapping_id
        """).execute()
      }
    }

    def flush() : Unit = {
      
      // Insert Weights
      log.debug(s"Storing num=${weights.size} relation=weights")
      writeWeights(weights.values)
      weights.clear()

      // Insert Factors 
      log.debug(s"Storing num=${factors.size} relation=factors")
      writeFactors(factors)

      // Insert Variables
      val numEvidenceVariables = variables.values.count(_.isEvidence)
      val numQueryVariables = variables.values.count(_.isQuery)
      log.debug(s"Storing num=${variables.size} num_evidence=${numEvidenceVariables} " +
        s"num_query=${numQueryVariables} relation=variables")
      writeVariables(variables)
      // Clear variables
      variables.clear()

      // Insert Factor Variables
      val factorVariables = factors.flatMap(_.variables)
      log.debug(s"Storing num=${factorVariables.size} relation=factor_variables")
      writeFactorVariables(factorVariables)
      factors.clear()
    }

    private def writeWeights(values: Iterable[Weight]) {
      val sqlStatement = """COPY weights(id, initial_value, is_fixed) FROM STDIN CSV"""
      copyBatchData(sqlStatement, toCSVData(values.iterator))
    }

    private def writeFactors(values: Iterable[Factor]) {
      val sqlStatement = """COPY factors(id, weight_id, factor_function) FROM STDIN CSV"""
      copyBatchData(sqlStatement, toCSVData(values.iterator))
    }

    private def writeVariables(values: Iterable[(VariableMappingKey, Variable)]) {
      val sqlStatement = """COPY variables(id, data_type, initial_value, is_evidence, is_query,
        mapping_relation, mapping_id, mapping_column) FROM STDIN CSV"""
      
      val varCSV = toCSVData(values.map(_._2).iterator)
      val mappingCSV = toCSVData(values.map(_._1).iterator)
      val fullCSV = varCSV.lines.zip(mappingCSV.lines).map { case(x,y) => s"$x,$y" }
      copyBatchData(sqlStatement, fullCSV.mkString("\n"))
    }

    private def writeFactorVariables(values: Iterable[FactorVariable]) {
      val sqlStatement = """COPY factor_variables(factor_id, variable_id, position, is_positive) FROM STDIN CSV"""
      copyBatchData(sqlStatement, toCSVData(values.iterator))
    }

    private def writeBatch(sqlStatement: SqlQuery, values: Iterator[AnormSeq]) {
      values.grouped(BATCH_SIZE).zipWithIndex.foreach { case(batch, i) =>
        val batchInsert = new BatchSql(sqlStatement, batch.toSeq)
        batchInsert.execute()
      }
    }

    private def toCSVData[T <: CSVFormattable](data: Iterator[T]) : String = {
      val strWriter = new StringWriter
      val writer = new CSVWriter(strWriter)
      data.foreach (obj => writer.writeNext(obj.toCSVRow))
      writer.close()
      strWriter.toString
    }

    private def copyBatchData(sqlStatement: String, rawData: String) {
      // We use Postgres' copy manager isntead of anorm to do efficient batch inserting
      // Do some magic to ge the underlying connection
      val del = new org.apache.commons.dbcp.DelegatingConnection(connection)
      val pg_conn = del.getInnermostDelegate().asInstanceOf[org.postgresql.core.BaseConnection]
      val cm = new org.postgresql.copy.CopyManager(pg_conn)
      val is = new ByteArrayInputStream(rawData.getBytes)
      cm.copyIn(sqlStatement, is)
    }

  }
}