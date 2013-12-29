package org.deepdive.inference

import anorm._
import au.com.bytecode.opencsv.CSVWriter
import java.io.{ByteArrayInputStream, File, FileOutputStream, StringWriter}
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import org.deepdive.datastore.PostgresDataStore
import org.deepdive.Logging
import scala.collection.mutable.{Map, ArrayBuffer, Set}
import scala.io.Source

/* Stores the factor graph */
trait PostgresInferenceDataStoreComponent extends InferenceDataStoreComponent {

  lazy val inferenceDataStore = new PostgresInferenceDataStore

  class PostgresInferenceDataStore extends InferenceDataStore with Logging {

    implicit lazy val connection = PostgresDataStore.borrowConnection()

    val BatchSize = Some(50000)

    // We keep track of the variables and weights we have already added
    // These will be kept in memory
    val variableIdSet = Collections.newSetFromMap[Long](
      new ConcurrentHashMap[Long, java.lang.Boolean]())
    val weightIdMap = new ConcurrentHashMap[String, Long]()
    
    // Temorary buffer for the next batch
    // These collections will the cleared when we write the next batch to postgres
    val variables = ArrayBuffer[Variable]()
    val factors = ArrayBuffer[Factor]()
    val weights = Map[String, Weight]()
    

    def init() : Unit = {
      // weights(id, initial_value, is_fixed)
      SQL("""drop table if exists weights; 
        create table weights(id bigint primary key, 
        initial_value double precision, is_fixed boolean, description text);""").execute()
      
      // factors(id, weight_id, factor_function)
      SQL("""drop table if exists factors CASCADE; 
        create table factors(id bigint primary key, 
        weight_id bigint, factor_function text);""").execute()

      // variables(id, data_type, initial_value, is_evidence, is_query, mapping_relation, mapping_column, mapping_id)
      SQL("""drop table if exists variables CASCADE; 
        create table variables(id bigint primary key, data_type text,
        initial_value double precision, is_evidence boolean, is_query boolean,
        mapping_relation text, mapping_column text);""").execute()
      
      // factor_variables(factor_id, variable_id, position, is_positive)
      SQL("""drop table if exists factor_variables; 
        create table factor_variables(factor_id bigint, variable_id bigint, 
        position int, is_positive boolean);""").execute()
      SQL("CREATE INDEX ON factor_variables using hash (factor_id);").execute()
      SQL("CREATE INDEX ON factor_variables using hash (variable_id);").execute()

      // inference_result(id, last_sample, probability)
      SQL("""drop table if exists inference_result CASCADE; 
        create table inference_result(id bigint primary key, last_sample boolean, 
        probability double precision);""").execute()
      SQL("CREATE INDEX ON inference_result using btree (probability);").execute()

      // A view for the mapped inference result
      SQL("""drop view if exists mapped_inference_result; 
      CREATE VIEW mapped_inference_result AS SELECT variables.*, inference_result.last_sample, inference_result.probability 
      FROM variables INNER JOIN inference_result ON variables.id = inference_result.id;
      """).execute()
    }

    def addFactor(factor: Factor) : Unit = {
      factors += factor
    }

    def addVariable(variable: Variable) : Unit = {
      variables += variable
      variableIdSet.add(variable.id)
    }

    def hasVariable(id: Long) : Boolean = variableIdSet.contains(id)

    def getWeightId(identifier: String) : Option[Long] = Option(weightIdMap.get(identifier))

    def addWeight(identifier: String, weight: Weight) = { 
      if (!weightIdMap.contains(identifier)) {
        weights += Tuple2(identifier, weight)
        weightIdMap.put(identifier, weight.id)
      }
    }

    def dumpFactorGraph(factorMapFile: File, factorsFile: File, weightsFile: File) : Unit = {
      // Write weights
      log.info(s"Writing weights to file=${weightsFile.getAbsolutePath}")
      copySQLToTSV("""SELECT id, initial_value, 
        case when is_fixed then 'true' else 'false' end,
        description
        FROM weights""", weightsFile)

      // Write factors
      log.info(s"Writing factors to file=${factorsFile.getAbsolutePath}")
      copySQLToTSV("SELECT id, weight_id, factor_function FROM factors", factorsFile)

      // Write variables
      log.info(s"Writing factor_map to file=${factorMapFile.getAbsolutePath}")
      copySQLToTSV("""SELECT variables.id, factor_variables.factor_id, factor_variables.position,
      case when factor_variables.is_positive then 'true' else 'false' end, 
      variables.data_type, variables.initial_value, 
      case when variables.is_evidence then 'true' else 'false' end,
      case when variables.is_query then 'true' else 'false' end
      FROM variables LEFT JOIN factor_variables ON factor_variables.variable_id = variables.id""", 
      factorMapFile)

    }


    def writeInferenceResult(file: String) : Unit = {
      // Copy the inference result back to the database
      copyBatchData("COPY inference_result(id, last_sample, probability) FROM STDIN",
        Source.fromFile(file).getLines.mkString("\n"))
      
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
            (SELECT mir.last_sample, mir.probability, mir.id 
            FROM mapped_inference_result mir 
            WHERE mapping_relation = '${relationName}' AND mapping_column = '${columnName}') 
          mir ON ${relationName}.id = mir.id
        """).execute()
      }
    }

    def flush() : Unit = {
      
      // Insert Weights
      log.debug(s"Storing num=${weights.size} relation=weights")
      copyBatchData("""COPY weights(id, initial_value, is_fixed, description) FROM STDIN CSV""", 
        toCSVData(weights.values.iterator))
      weights.clear()

      // Insert Factors 
      log.debug(s"Storing num=${factors.size} relation=factors")
      copyBatchData( """COPY factors(id, weight_id, factor_function) FROM STDIN CSV""", 
        toCSVData(factors.iterator))

      // Insert Variables
      val numEvidence = variables.count(_.isEvidence)
      val numQuery = variables.count(_.isQuery)
      log.debug(s"Storing num=${variables.size} num_evidence=${numEvidence} " +
        s"num_query=${numQuery} relation=variables")
      copyBatchData("""COPY variables( id, data_type, initial_value, is_evidence, is_query,
        mapping_relation, mapping_column) FROM STDIN CSV""", 
        toCSVData(variables.iterator))
      // Clear variables
      variables.clear()

      // Insert Factor Variables
      copyBatchData("""COPY factor_variables(factor_id, variable_id, position, is_positive) 
        FROM STDIN CSV""", toCSVData(factors.iterator.flatMap(_.variables)))
      factors.clear()
    }

    private def toCSVData[T <: CSVFormattable](data: Iterator[T]) : String = {
      val strWriter = new StringWriter
      val writer = new CSVWriter(strWriter)
      data.foreach (obj => writer.writeNext(obj.toCSVRow))
      writer.close()
      strWriter.toString
    }

    private def copyBatchData(sqlStatement: String, rawData: String) {
      val del = new org.apache.commons.dbcp.DelegatingConnection(connection)
      val pg_conn = del.getInnermostDelegate().asInstanceOf[org.postgresql.core.BaseConnection]
      val cm = new org.postgresql.copy.CopyManager(pg_conn)
      val is = new ByteArrayInputStream(rawData.getBytes)
      cm.copyIn(sqlStatement, is)
    }

    private def copySQLToTSV(sqlSelect: String, f: File) {
      val del = new org.apache.commons.dbcp.DelegatingConnection(connection)
      val pg_conn = del.getInnermostDelegate().asInstanceOf[org.postgresql.core.BaseConnection]
      val cm = new org.postgresql.copy.CopyManager(pg_conn)
      val os = new FileOutputStream(f)
      val copySql = s"COPY ($sqlSelect) TO STDOUT"
      cm.copyOut(copySql, os)
      os.close()
    }

  }
}