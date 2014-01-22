package org.deepdive.inference

import anorm._
import au.com.bytecode.opencsv.CSVWriter
import java.io.{ByteArrayInputStream, File, FileOutputStream, FileWriter,
  StringWriter, Reader, FileReader, InputStream, InputStreamReader}
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import org.deepdive.settings.FactorFunctionVariable
import org.deepdive.datastore.PostgresDataStore
import org.deepdive.Logging
import org.deepdive.calibration._
import scala.collection.mutable.{ArrayBuffer, Set}
import scala.io.Source

/* Stores the factor graph and inference results in a postges database. */
trait PostgresInferenceDataStoreComponent extends InferenceDataStoreComponent {

  lazy val inferenceDataStore = new PostgresInferenceDataStore

  class PostgresInferenceDataStore extends InferenceDataStore with Logging {

    implicit lazy val connection = PostgresDataStore.borrowConnection()

    // Default batch size, if not overwritten by user
    val BatchSize = Some(50000)

    // We keep track of the variables, weights and factors already added
    // These will be kept in memory at all times.
    // TODO: Ideally, we don't keep anything in memory and resolve conflicts in the database.
    val variableIdSet = Collections.newSetFromMap[Long](
      new ConcurrentHashMap[Long, java.lang.Boolean]())
    val weightIdSet = Collections.newSetFromMap[Long](
      new ConcurrentHashMap[Long, java.lang.Boolean]())
    val factorIdSet = Collections.newSetFromMap[Long](
      new ConcurrentHashMap[Long, java.lang.Boolean]())
    
    // Temorary buffer for the next batch.
    // These collections will the cleared when we write the next batch to postgres
    val variables = ArrayBuffer[Variable]()
    val factors = ArrayBuffer[Factor]()
    val weights = ArrayBuffer[Weight]()
    
    def init() : Unit = {
      
      variableIdSet.clear()
      weightIdSet.clear()
      factorIdSet.clear()
      variables.clear()
      factors.clear()
      weights.clear()

      // weights(id, initial_value, is_fixed, description)
      SQL("""drop table if exists weights CASCADE; 
        create table weights(id bigint primary key, 
        initial_value double precision, is_fixed boolean, description text);""").execute()
      
      // factors(id, weight_id, factor_function)
      SQL("""drop table if exists factors CASCADE; 
        create table factors(id bigint primary key, 
        weight_id bigint, factor_function text);""").execute()

      // variables(id, data_type, initial_value, is_evidence, is_query, mapping_relation, mapping_column)
      SQL("""drop table if exists variables CASCADE; 
        create table variables(id bigint primary key, data_type text,
        initial_value double precision, is_evidence boolean, is_query boolean,
        mapping_relation text, mapping_column text, mapping_id bigint);""").execute()
      
      // factor_variables(factor_id, variable_id, position, is_positive)
      SQL("""drop table if exists factor_variables; 
        create table factor_variables(factor_id bigint, variable_id bigint, 
        position int, is_positive boolean);""").execute()
      SQL("CREATE INDEX factor_idx ON factor_variables (factor_id);").execute()
      SQL("CREATE INDEX factor_variables_idx ON factor_variables (variable_id);").execute()
      SQL("analyze").execute()

      // inference_result(id, last_sample, probability)
      SQL("""drop table if exists inference_result CASCADE; 
        create table inference_result(id bigint primary key, last_sample boolean, 
        probability double precision);""").execute()

      // inference_result_weights(id, weight)
      SQL("""drop table if exists inference_result_weights CASCADE; 
        create table inference_result_weights(id bigint primary key, 
          weight double precision);""").execute()

      // A view for the mapped inference result.
      // The view is a join of the variables and inference result tables.
      SQL("""drop view if exists mapped_inference_result; 
      CREATE VIEW mapped_inference_result AS SELECT variables.*, inference_result.last_sample, inference_result.probability 
      FROM variables INNER JOIN inference_result ON variables.id = inference_result.id;
      """).execute()
    }

    def getLocalVariableIds(rowMap: Map[String, Any], factorVar: FactorFunctionVariable) : Array[Long] = {
      if (factorVar.isArray)
        // Postgres prefixes aggregated colimns with a dot
        rowMap(s".${factorVar.relation}.id").asInstanceOf[Array[Long]]
      else
        Array(rowMap(s"${factorVar.relation}.id").asInstanceOf[Long])
    }

    def addFactor(factor: Factor) = { 
      if (!factorIdSet.contains(factor.id)) {
        factors += factor
        factorIdSet.add(factor.id)
      }
    }

    def addVariable(variable: Variable) = {
      if (!variableIdSet.contains(variable.id)) {
        variables += variable
        variableIdSet.add(variable.id)
      }
    }
    
    def addWeight(weight: Weight) = { 
      if (!weightIdSet.contains(weight.id)) {
        weights += weight
        weightIdSet.add(weight.id)
      }
    }

    def dumpFactorGraph(variablesFile: File, factorsFile: File, weightsFile: File) : Unit = {
      // Write the weights file
      log.info(s"Writing weights to file=${weightsFile.getAbsolutePath}")
      copySQLToFile("""SELECT id, initial_value, 
        case when is_fixed then 'true' else 'false' end,
        description
        FROM weights""", weightsFile)

      // Write factors file
      log.info(s"Writing factors to file=${factorsFile.getAbsolutePath}")
      copySQLToFile("SELECT id, weight_id, factor_function FROM factors", factorsFile)

      // Write variables file
      log.info(s"Writing factor_map to file=${variablesFile.getAbsolutePath}")
      copySQLToFile("""SELECT variables.id, factor_variables.factor_id, factor_variables.position,
        case when factor_variables.is_positive then 'true' else 'false' end, 
        variables.data_type, variables.initial_value, 
        case when variables.is_evidence then 'true' else 'false' end,
        case when variables.is_query then 'true' else 'false' end
        FROM variables LEFT JOIN factor_variables ON factor_variables.variable_id = variables.id""", 
      variablesFile)

    }

    def writebackInferenceResult(variableSchema: Map[String, String],
      variableOutputFile: String, weightsOutputFile: String) = {
      log.info("writing back inference result")

      // Copy the inference result back to the database
      PostgresDataStore.copyBatchData("COPY inference_result(id, last_sample, probability) FROM STDIN",
        Source.fromFile(variableOutputFile).reader())
      SQL("CREATE INDEX inference_result_idx ON inference_result using btree (probability);").execute()
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
        SQL(s"""DROP VIEW IF EXISTS ${view_name}; CREATE VIEW ${view_name} AS
          SELECT ${relationName}.*, mir.last_sample, mir.probability FROM
          ${relationName} JOIN
            (SELECT mir.last_sample, mir.probability, mir.id, mir.mapping_id 
            FROM mapped_inference_result mir 
            WHERE mapping_relation = '${relationName}' AND mapping_column = '${columnName}'
            ORDER BY mir.probability DESC) 
          mir ON ${relationName}.id = mir.mapping_id""").execute()
      }

      // Copy the weight result back to the database
      PostgresDataStore.copyBatchData("COPY inference_result_weights(id, weight) FROM STDIN",
        Source.fromFile(weightsOutputFile).reader())
      SQL("""CREATE INDEX inference_result_weights_idx 
        ON inference_result_weights using btree (weight);""").execute()
      SQL("analyze").execute()
      
      // Create a view that maps weight descriptions to the weight values
      val mappedVeightsView = "inference_result_mapped_weights"
      log.info(s"creating view=${mappedVeightsView}")
      SQL(s"""create or replace view ${mappedVeightsView} AS
        SELECT weights.*, inference_result_weights.weight FROM
        weights JOIN inference_result_weights ON weights.id = inference_result_weights.id
        ORDER BY abs(weight) DESC""").execute()
      
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
      val weightsCSV : File = toCSVData(weights.iterator)
      PostgresDataStore.copyBatchData("""COPY weights(id, initial_value, is_fixed, description) FROM STDIN CSV""", 
        weightsCSV)
      weightsCSV.delete()
      
      // Insert variables
      val numEvidence = variables.count(_.isEvidence)
      val numQuery = variables.count(_.isQuery)
      log.debug(s"Storing num_variables=${variables.size} num_evidence=${numEvidence} " +
        s"num_query=${numQuery}")
      val variablesCSV = toCSVData(variables.iterator)
      PostgresDataStore.copyBatchData("""COPY variables( id, data_type, initial_value, is_evidence, is_query,
        mapping_relation, mapping_column, mapping_id) FROM STDIN CSV""", variablesCSV)
      variablesCSV.delete()
      
      // Insert factors 
      log.debug(s"Storing num_factors=${factors.size}")
      val factorsCSV = toCSVData(factors.iterator)
      PostgresDataStore.copyBatchData( """COPY factors(id, weight_id, factor_function) FROM STDIN CSV""", 
        factorsCSV)
      factorsCSV.delete()
      
      // Insert Factor Variables
      val factorVarCSV = toCSVData(factors.iterator.flatMap(_.variables))
      PostgresDataStore.copyBatchData("""COPY factor_variables(factor_id, variable_id, position, is_positive) 
        FROM STDIN CSV""", factorVarCSV)
      factorVarCSV.delete()
      
      // Clear the temporary buffer
      weights.clear()
      factors.clear()
      variables.clear()
    }

    // Converts CSV-formattable data to a CSV string
    private def toCSVData[T <: CSVFormattable](data: Iterator[T]) : File = {
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
