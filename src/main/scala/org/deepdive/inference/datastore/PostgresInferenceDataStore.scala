package org.deepdive.inference

import anorm._
import au.com.bytecode.opencsv.CSVWriter
import java.io.{ByteArrayInputStream, File, FileOutputStream, FileWriter,
  StringWriter, Reader, FileReader, InputStream, InputStreamReader}
import org.deepdive.settings.FactorFunctionVariable
import org.deepdive.datastore.PostgresDataStore
import org.deepdive.Logging
import org.deepdive.calibration._
import scala.collection.mutable.{ArrayBuffer, Set, SynchronizedBuffer}
import scala.io.Source

/* Stores the factor graph and inference results in a postges database. */
trait PostgresInferenceDataStoreComponent extends InferenceDataStoreComponent {

  lazy val inferenceDataStore = new PostgresInferenceDataStore

  class PostgresInferenceDataStore extends InferenceDataStore with Logging {

    implicit lazy val connection = PostgresDataStore.borrowConnection()

    // Table names
    val WeightsTable = "dd_graph_weights"
    val FactorsTable = "dd_graph_factors"
    val VariablesTable = "dd_graph_variables"
    val EdgesTable = "dd_graph_edges"
    val WeightResultTable = "dd_inference_result_weights"
    val VariableResultTable = "dd_inference_result_variables"
    val MappedInferenceResultView = "dd_mapped_inference_result"


    // Default batch size, if not overwritten by user
    val BatchSize = Some(250000)
    
    // Temorary buffer for the next batch.
    // These collections will the cleared when we write the next batch to postgres
    val variables = new ArrayBuffer[Variable] with SynchronizedBuffer[Variable]
    val factors = new ArrayBuffer[Factor] with SynchronizedBuffer[Factor]
    val weights = new ArrayBuffer[Weight] with SynchronizedBuffer[Weight]
    
    def init() : Unit = {
      
      variables.clear()
      factors.clear()
      weights.clear()

      // weights(id, initial_value, is_fixed, description)
      SQL(s"""drop table if exists ${WeightsTable} CASCADE; 
        create table ${WeightsTable}(id bigint primary key, 
        initial_value double precision, is_fixed boolean, description text);""").execute()
      
      // factors(id, weight_id, factor_function)
      SQL(s"""drop table if exists ${FactorsTable} CASCADE; 
        create table ${FactorsTable}(id bigint primary key, 
        weight_id bigint, factor_function text);""").execute()

      // variables(id, data_type, initial_value, is_evidence, is_query, mapping_relation, mapping_column)
      SQL(s"""drop table if exists ${VariablesTable} CASCADE; 
        create table ${VariablesTable}(id bigint primary key, data_type text,
        initial_value double precision, is_evidence boolean, is_query boolean,
        mapping_relation text, mapping_column text, mapping_id bigint);""").execute()
      
      // factor_variables(factor_id, variable_id, position, is_positive)
      SQL(s"""drop table if exists ${EdgesTable}; 
        create table ${EdgesTable}(
        factor_id bigint, 
        variable_id bigint, 
        position int, is_positive boolean);""").execute()

      // inference_result(id, last_sample, probability)
      SQL(s"""drop table if exists ${VariableResultTable} CASCADE; 
        create table ${VariableResultTable}(id bigint primary key, last_sample boolean, 
        probability double precision);""").execute()

      // inference_result_weights(id, weight)
      SQL(s"""drop table if exists ${WeightResultTable} CASCADE; 
        create table ${WeightResultTable}(id bigint primary key, 
          weight double precision);""").execute()

      // A view for the mapped inference result.
      // The view is a join of the variables and inference result tables.
      SQL(s"""CREATE OR REPLACE VIEW ${MappedInferenceResultView} 
        AS SELECT ${VariablesTable}.*, ${VariableResultTable}.last_sample, ${VariableResultTable}.probability 
        FROM ${VariablesTable} 
          INNER JOIN ${VariableResultTable} ON ${VariablesTable}.id = ${VariableResultTable}.id;
      """).execute()

      SQL("analyze").execute()
    }

    def getLocalVariableIds(rowMap: Map[String, Any], factorVar: FactorFunctionVariable) : Array[Long] = {
      if (factorVar.isArray)
        // Postgres prefixes aggregated colimns with a dot
        rowMap(s".${factorVar.relation}.id").asInstanceOf[Array[java.lang.Long]].map(_.toLong)
      else
        Array(rowMap(s"${factorVar.relation}.id").asInstanceOf[Long])
    }

    def addFactor(factor: Factor) = { 
      factors += factor
    }

    def addVariable(variable: Variable) = {
      variables += variable
    }
    
    def addWeight(weight: Weight) = { 
      weights += weight
    }

    def dumpFactorGraph(serializer: Serializer) : Unit = {
      // Add all weights
      log.info(s"Dumping factor graph...")
      log.info("Serializing weights...")
      SQL(s"SELECT * from ${WeightsTable} order by id asc")().iterator.foreach { row =>
        serializer.addWeight(
          row[Long]("id"),
          row[Boolean]("is_fixed"),
          row[Double]("initial_value"),
          row[String]("description")
        )
      }
      // Add all variables
      log.info("Serializing variables...")
      SQL(s"SELECT * from ${VariablesTable} order by id asc")().iterator.foreach { row =>
        serializer.addVariable(
          row[Long]("id"),
          if (row[Boolean]("is_evidence")) row[Option[Double]]("initial_value") else None,
          row[String]("data_type")
        )
      }
      // Add all factors
      log.info("Serializing factors...")
      SQL(s"SELECT * from ${FactorsTable} order by id asc")().iterator.foreach { row =>
        serializer.addFactor(
          row[Long]("id"),
          row[Long]("weight_id"),
          row[String]("factor_function")
        )
      }
      // Add all edges
      log.info("Serializing edges...")
      SQL(s"SELECT * from ${EdgesTable}")().iterator.foreach { row =>
        serializer.addEdge(
          row[Long]("variable_id"),
          row[Long]("factor_id"),
          row[Long]("position"),
          row[Boolean]("is_positive")
        )
      }
      log.info("Writing serialization result...")
      serializer.close()
    }

    def writebackInferenceResult(variableSchema: Map[String, String],
      variableOutputFile: String, weightsOutputFile: String) = {
      log.info("writing back inference result")

      // Create useful indicies
      SQL(s"CREATE INDEX ${FactorsTable}_weight_id_idx ON ${FactorsTable} (weight_id);").execute()
      SQL(s"CREATE INDEX ${EdgesTable}_factor_id_idx ON ${EdgesTable} (factor_id);").execute()
      SQL(s"CREATE INDEX ${EdgesTable}_variable_id_idx ON ${EdgesTable} (variable_id);").execute()

      // Copy the inference result back to the database
      PostgresDataStore.copyBatchData(s"COPY ${VariableResultTable}(id, last_sample, probability) FROM STDIN",
        Source.fromFile(variableOutputFile).reader())
      SQL(s"CREATE INDEX ${VariableResultTable}_idx ON ${VariableResultTable} using btree (probability);").execute()
      SQL("analyze").execute()

      // Each (relation, column) tuple is a variable in the plate model.
      // Find all (relation, column) combinations
      val relationsColumns = variableSchema.keys map (_.split('.')) map {
        case Array(relation, column) => (relation, column)
      } 

      // Generate a view for each (relation, column) combination.
      relationsColumns.foreach { case(relationName, columnName) => 
        val view_name = s"${relationName}_${columnName}_inference"
        log.info(s"creating view=${view_name}")
        SQL(s"""CREATE OR REPLACE VIEW ${view_name} AS
          SELECT ${relationName}.*, mir.last_sample, mir.probability FROM
          ${relationName} JOIN
            (SELECT mir.last_sample, mir.probability, mir.id, mir.mapping_id 
            FROM ${MappedInferenceResultView} mir 
            WHERE mapping_relation = '${relationName}' AND mapping_column = '${columnName}'
            ORDER BY mir.probability DESC) 
          mir ON ${relationName}.id = mir.mapping_id""").execute()
      }


      // Copy the weight result back to the database
      PostgresDataStore.copyBatchData(s"COPY ${WeightResultTable}(id, weight) FROM STDIN",
        Source.fromFile(weightsOutputFile).reader())
      SQL(s"""CREATE INDEX ${WeightResultTable}_idx 
        ON ${WeightResultTable} using btree (weight);""").execute()
      SQL("analyze").execute()
      
      // Create a view that maps weight descriptions to the weight values
      val mappedVeightsView = s"${VariableResultTable}_mapped_weights"
      log.info(s"creating view=${mappedVeightsView}")
      SQL(s"""create or replace view ${mappedVeightsView} AS
        SELECT ${WeightsTable}.*, ${WeightResultTable}.weight FROM
        ${WeightsTable} JOIN ${WeightResultTable} ON ${WeightsTable}.id = ${WeightResultTable}.id
        ORDER BY abs(weight) DESC""").execute()

      relationsColumns.foreach { case(relationName, columnName) => 
        val view_name = s"${relationName}_${columnName}_weights"
        log.info(s"creating view=${view_name}")
        SQL(s"""CREATE OR REPLACE VIEW ${view_name} AS
            WITH top_weights AS (SELECT id
            FROM ${VariableResultTable}_mapped_weights 
            ORDER BY abs(weight) DESC),
          relevant_variables AS (SELECT top_weights.id, is_evidence, initial_value FROM top_weights 
            INNER JOIN ${FactorsTable} ON ${FactorsTable}.weight_id = top_weights.id 
            INNER JOIN ${EdgesTable} ON ${FactorsTable}.id = ${EdgesTable}.factor_id 
            INNER JOIN ${VariablesTable} ON ${EdgesTable}.variable_id = ${VariablesTable}.id
            WHERE ${VariablesTable}.mapping_relation = '${relationName}' 
              AND ${VariablesTable}.mapping_column = '${columnName}'),
          grouped_weights AS (SELECT relevant_variables.id,
            COUNT(CASE WHEN relevant_variables.is_evidence=true 
              AND relevant_variables.initial_value=1.0 THEN 1 END) AS true_count,
            COUNT(CASE WHEN relevant_variables.is_evidence=true 
              AND relevant_variables.initial_value=0.0 THEN 1 END) AS false_count,
            COUNT(CASE WHEN relevant_variables.is_evidence=false THEN 1 END) AS total_count
            FROM relevant_variables GROUP BY (id))
          SELECT description, weight, true_count, false_count, total_count
            FROM grouped_weights INNER JOIN ${VariableResultTable}_mapped_weights 
              ON grouped_weights.id = ${VariableResultTable}_mapped_weights.id
            ORDER BY abs(weight) DESC""").execute()
      }
      
    }

    def getCalibrationData(variable: String, buckets: List[Bucket]) : Map[Bucket, BucketData] = {
      val Array(relationName, columnName) = variable.split('.')
      val inference_view = s"${relationName}_${columnName}_inference"
      val bucketed_view = s"${relationName}_${columnName}_inference_bucketed"
      val calibration_view = s"${relationName}_${columnName}_calibration"
      
      // Generate a temporary bucketed view
      val bucketCaseStatement = buckets.zipWithIndex.map { case(bucket, index) =>
        s"when probability >= ${bucket.from} and probability <= ${bucket.to} then ${index}"
      }.mkString("\n")
      SQL(s"""create or replace view ${bucketed_view} AS
        SELECT ${inference_view}.*, case ${bucketCaseStatement} end bucket
        FROM ${inference_view} ORDER BY bucket ASC""").execute()
      log.info(s"created bucketed_inference_view=${bucketed_view}")

      // Generate the calibration view
      SQL(s"""create or replace view ${calibration_view} AS
        SELECT b1.bucket, b1.numVariables, b2.numTrue, b3.numFalse FROM
        (SELECT bucket, COUNT(*) AS numVariables from ${bucketed_view} GROUP BY bucket) b1
        LEFT JOIN (SELECT bucket, COUNT(*) AS numTrue from ${bucketed_view} 
          WHERE ${columnName}=true GROUP BY bucket) b2 ON b1.bucket = b2.bucket
        LEFT JOIN (SELECT bucket, COUNT(*) AS numFalse from ${bucketed_view} 
          WHERE ${columnName}=false GROUP BY bucket) b3 ON b1.bucket = b3.bucket 
        ORDER BY b1.bucket ASC""").execute()

      log.info(s"created calibration_view=${calibration_view}")

      // Build and Return the final calibration data
      val bucketData = SQL(s"SELECT * from ${calibration_view}")().map { row =>
        val bucket = row[Long]("bucket")
        val data = BucketData(row[Option[Long]]("numVariables").getOrElse(0),
          row[Option[Long]]("numTrue").getOrElse(0), 
          row[Option[Long]]("numFalse").getOrElse(0))
        (bucket, data)
      }.toMap

      buckets.zipWithIndex.map { case (bucket, index) =>
        (bucket, bucketData.get(index).getOrElse(BucketData(0,0,0)))
      }.toMap
    }

    def flush() : Unit = {

      // Insert weight
      log.debug(s"Storing num_weights=${weights.size}")
      val weightsCSV : File = toCSVData(weights)
      log.debug(s"Wrote weights_csv_file=${weightsCSV.getCanonicalPath}")
      PostgresDataStore.copyBatchData(s"""COPY ${WeightsTable}(id, initial_value, is_fixed, description) FROM STDIN CSV""", 
        weightsCSV)
      log.debug(s"Stored num_weights=${weights.size}")
      
      // Insert variables
      val numEvidence = variables.count(_.isEvidence)
      val numQuery = variables.count(_.isQuery)
      log.debug(s"Storing num_variables=${variables.size} num_evidence=${numEvidence} " +
        s"num_query=${numQuery}")
      val variablesCSV = toCSVData(variables)
      log.debug(s"Wrote variables_csv_file=${variablesCSV.getCanonicalPath}")
      PostgresDataStore.copyBatchData(s"""COPY ${VariablesTable}(id, data_type, initial_value, is_evidence, is_query,
        mapping_relation, mapping_column, mapping_id) FROM STDIN CSV""", variablesCSV)
      log.debug(s"Stored num_variables=${variables.size}")
      
      
      // Insert factors 
      log.debug(s"Storing num_factors=${factors.size}")
      val factorsCSV = toCSVData(factors)
      log.debug(s"Wrote factors_csv_file=${factorsCSV.getCanonicalPath}")
      PostgresDataStore.copyBatchData(s"""COPY ${FactorsTable}(id, weight_id, factor_function) FROM STDIN CSV""", 
        factorsCSV)
      log.debug(s"Stored num_factors=${factors.size}")
      
      // Insert Factor Variables
      val edges = factors.flatMap(_.variables)
      log.debug(s"Storing num_edges=${edges.size}")
      val edgeCSV = toCSVData(edges)
      log.debug(s"Wrote edge_csv_file=${edgeCSV.getCanonicalPath}")
      PostgresDataStore.copyBatchData(s"""COPY ${EdgesTable}(factor_id, variable_id, position, is_positive) 
        FROM STDIN CSV""", edgeCSV)
      log.debug(s"Stored num_edges=${edges.size}")
      
      // Remove tmp files
      variablesCSV.delete()
      factorsCSV.delete()
      edgeCSV.delete()
      weightsCSV.delete()

      // Clear the temporary buffer
      weights.clear()
      factors.clear()
      variables.clear()
    }

    // Converts CSV-formattable data to a CSV string
    private def toCSVData[T <: CSVFormattable](data: Iterable[T]) : File = {
      val tmpFile = File.createTempFile("csv", "")
      val writer = new CSVWriter(new FileWriter(tmpFile))
      data.foreach (obj => writer.writeNext(obj.toCSVRow))
      writer.close()
      tmpFile
    }

    // Executes a SELECT statement and saves the result in a postgres text format file
    // (http://www.postgresql.org/docs/9.1/static/sql-copy.html#AEN64107)
    private def copySQLToFile(sqlSelect: String, f: File) = {
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
