package org.deepdive.inference

import anorm._
import au.com.bytecode.opencsv.CSVWriter
import java.io.{ByteArrayInputStream, File, FileOutputStream, FileWriter,
  StringWriter, Reader, FileReader, InputStream, InputStreamReader}
import org.deepdive.calibration._
import org.deepdive.datastore.PostgresDataStore
import org.deepdive.Logging
import org.deepdive.settings._
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
    val LocalVariableMapTable = "dd_graph_local_variable_map"
    val EdgesTable = "dd_graph_edges"
    val WeightResultTable = "dd_inference_result_weights"
    val VariableResultTable = "dd_inference_result_variables"
    val MappedInferenceResultView = "dd_mapped_inference_result"


    // Default batch size, if not overwritten by user
    val BatchSize = Some(250000)
    
    def init() : Unit = {

      // weights(id, initial_value, is_fixed, description)
      SQL(s"""drop table if exists ${WeightsTable} CASCADE; 
        create table ${WeightsTable}(id bigserial primary key, 
        initial_value double precision, is_fixed boolean, description text);""").execute()
      SQL(s"""ALTER SEQUENCE ${WeightsTable}_id_seq MINVALUE -1 RESTART WITH 0;""").execute()
      SQL(s"""CREATE INDEX ${WeightsTable}_desc_idx ON ${WeightsTable}(description);""").execute()
      
      // factors(id, weight_id, factor_function)
      SQL(s"""drop table if exists ${FactorsTable} CASCADE; 
        create table ${FactorsTable}(id bigserial primary key, 
        weight_id bigint, factor_function text, factor_group text, qid bigint);""").execute()
      SQL(s"""ALTER SEQUENCE ${FactorsTable}_id_seq MINVALUE -1 RESTART WITH 0;""").execute()

      // variables(id, data_type, initial_value, is_evidence, is_query, mrel, mcol)
      SQL(s"""drop table if exists ${VariablesTable} CASCADE; 
        create table ${VariablesTable}(id bigserial primary key, data_type text,
        initial_value double precision, is_evidence boolean);""").execute()
      SQL(s"""ALTER SEQUENCE ${VariablesTable}_id_seq MINVALUE -1 RESTART WITH 0;""").execute()

      SQL(s"""DROP TABLE IF EXISTS ${LocalVariableMapTable} CASCADE;
        CREATE TABLE ${LocalVariableMapTable}(id bigserial,
          mrel text, mcol text, mid bigint,
          CONSTRAINT c_pkey PRIMARY KEY (mrel, mcol, mid));""").execute()
      SQL(s"""ALTER SEQUENCE ${LocalVariableMapTable}_id_seq MINVALUE -1 RESTART WITH 0;""").execute()
      
      // factor_variables(factor_id, variable_id, position, is_positive)
      SQL(s"""drop table if exists ${EdgesTable}; 
        create table ${EdgesTable}(
        factor_id bigint, 
        variable_id bigint, 
        position int, is_positive boolean);""").execute()

      // inference_result(id, category, expectation)
      SQL(s"""drop table if exists ${VariableResultTable} CASCADE; 
        create table ${VariableResultTable}(id bigint, category bigint, 
        expectation double precision);""").execute()

      // inference_result_weights(id, weight)
      SQL(s"""drop table if exists ${WeightResultTable} CASCADE; 
        create table ${WeightResultTable}(id bigint primary key, 
          weight double precision);""").execute()

      // A view for the mapped inference result.
      // The view is a join of the variables and inference result tables.
      SQL(s"""CREATE OR REPLACE VIEW ${MappedInferenceResultView} 
        AS SELECT ${VariablesTable}.*, ${LocalVariableMapTable}.mid, 
          ${LocalVariableMapTable}.mcol, ${LocalVariableMapTable}.mrel,
          ${VariableResultTable}.category, ${VariableResultTable}.expectation 
        FROM ${VariablesTable}, ${LocalVariableMapTable}, ${VariableResultTable}
        WHERE ${VariablesTable}.id = ${VariableResultTable}.id
          AND ${VariablesTable}.id = ${LocalVariableMapTable}.id;""").execute()

    }

    def groundFactorGraph(factorDesc: FactorDesc, holdoutFraction: Double) {

      val weightVariableSep = ","
      val weightVars = factorDesc.weight.variables.map(v => s""""${v}"::text""").mkString(", ") match {
        case "" => "''"
        case x => x
      }
      val weightValue = factorDesc.weight match { 
        case x : KnownFactorWeight => x.value
        case _ => 0.0
      }
      val weightIdCmd = s"""concat_ws('${weightVariableSep}', '${factorDesc.weightPrefix}', ${weightVars})"""

      // Materialize the query to get unique ids
      val queryViewTmp = s"dd_${factorDesc.name}_query_tmp"
      val queryView = s"dd_${factorDesc.name}_query"
      log.info("Materializing grounding query...")
      SQL(s"""
        DROP VIEW IF EXISTS ${queryViewTmp} CASCADE;""").execute()
      SQL(s"""
        CREATE OR REPLACE VIEW ${queryViewTmp} AS ${factorDesc.inputQuery};""").execute()
      SQL(s"""
        DROP MATERIALIZED VIEW IF EXISTS ${queryView} CASCADE;""").execute()
      SQL(s"""
        CREATE MATERIALIZED VIEW ${queryView} AS 
        SELECT row_number() OVER() - 1 as id, 
          ${queryViewTmp}.*, ${weightIdCmd} AS dd_weight 
          FROM ${queryViewTmp};""").execute()
      SQL(s"""CREATE INDEX ${queryView}_idx ON ${queryView}(id);""").execute()
      // Insert weights
      
      log.info("Inserting weights...")
      SQL(s"""
        INSERT INTO ${WeightsTable}(description, initial_value, is_fixed)
        (SELECT DISTINCT dd_weight, ${weightValue}, ${factorDesc.weight.isInstanceOf[KnownFactorWeight]}
        FROM ${queryView});""").execute()

      // Insert Factors
      val factorGroup = factorDesc.name
      val factorFunction = factorDesc.func.getClass.getSimpleName
      log.info("Inserting factors...")
      SQL(s"""
        INSERT INTO ${FactorsTable}(weight_id, factor_function, factor_group, qid)
        (SELECT ${WeightsTable}.id, '${factorFunction}', '${factorGroup}', ${queryView}.id
        FROM ${queryView}, ${WeightsTable}
        WHERE dd_weight = ${WeightsTable}.description);""").execute()


      // Insert variables, and edges
      factorDesc.func.variables.zipWithIndex.foreach { case(variable, position) =>
        // Insert local variables
        val vColumn = variable.field
        val vRelation = variable.headRelation
        val vidColumn = s"${variable.relation}.id"
        log.info(s"""Inserting variable="${variable.toString}"...""")
        SQL(s"""
          INSERT INTO ${LocalVariableMapTable}(mrel, mcol, mid)
          (SELECT DISTINCT '${vRelation}', '${vColumn}', "${vidColumn}"
          FROM ${queryView}
          EXCEPT SELECT '${vRelation}', '${vColumn}', mid 
            FROM ${LocalVariableMapTable}
            WHERE mrel='${vRelation}' AND mcol='${vColumn}');""").execute()

        // Insert Global Variables
        val variableDataType = factorDesc.func.variableDataType
        SQL(s"""
          INSERT INTO ${VariablesTable}(id, data_type, initial_value, is_evidence)
          SELECT DISTINCT ON (${LocalVariableMapTable}.id) ${LocalVariableMapTable}.id, '${variableDataType}', 
            CASE WHEN "${variable.toString}" = true THEN 1.0 ELSE 0.0 END,
            CASE WHEN ("${variable.toString}" IS NULL OR random() < ${holdoutFraction}) THEN false ELSE true END
          FROM 
            ${LocalVariableMapTable} LEFT JOIN ${VariablesTable} 
              ON ${LocalVariableMapTable}.id=${VariablesTable}.id,
            ${queryView}
          WHERE mrel='${vRelation}' 
            AND mcol='${vColumn}'
            AND mid="${vidColumn}"
            AND ${VariablesTable}.id IS NULL;""").execute()

        // Insert edges
        log.info(s"""Inserting edges for variable="${variable.toString}"...""")
        SQL(s"""
          INSERT INTO ${EdgesTable}(factor_id, variable_id, position, is_positive)
          (SELECT ${FactorsTable}.id, ${LocalVariableMapTable}.id, ${position}, ${!variable.isNegated}
          FROM ${queryView}, ${FactorsTable}, ${LocalVariableMapTable}
          WHERE ${queryView}.id = ${FactorsTable}.qid
            AND ${LocalVariableMapTable}.mrel = '${vRelation}'
            AND ${LocalVariableMapTable}.mcol = '${vColumn}'
            AND ${LocalVariableMapTable}.mid = "${vidColumn}"
            AND ${FactorsTable}.factor_group = '${factorGroup}')""").execute()
      }

    }

    def dumpFactorGraph(serializer: Serializer, schema: Map[String, _ <: VariableDataType]) : Unit = {
      // Add all weights
      log.info(s"Dumping factor graph...")
      log.info("Serializing weights...")
      SQL(s"SELECT * from ${WeightsTable} order by id asc")().par.foreach { row =>
        serializer.addWeight(
          row[Long]("id"),
          row[Boolean]("is_fixed"),
          row[Double]("initial_value"),
          row[String]("description")
        )
      }
      // Add all variables 
      schema.foreach { case(relationField, dataType) => 
        val Array(relationName, columnName) = relationField.split('.')
        log.info(s"""Serializing variable=" ${relationField} " """)
        SQL(s"""SELECT ${VariablesTable}.* FROM 
            ${VariablesTable} INNER JOIN ${LocalVariableMapTable} 
            ON ${VariablesTable}.id = ${LocalVariableMapTable}.id
            WHERE ${LocalVariableMapTable}.mrel = '${relationName}'
              AND ${LocalVariableMapTable}.mcol = '${columnName}'
            ORDER BY ${VariablesTable}.id asc""")().par.foreach { row =>
          serializer.addVariable(
            row[Long](s"id"),
            if (row[Boolean]("is_evidence")) row[Option[Double]]("initial_value") else None,
            dataType,
            dataType.cardinality
          )
        }
      }

      // Add all factors
      log.info("Serializing factors...")
      SQL(s"SELECT * from ${FactorsTable} order by id asc")().par.foreach { row =>
        serializer.addFactor(
          row[Long]("id"),
          row[Long]("weight_id"),
          row[String]("factor_function")
        )
      }
      // Add all edges
      log.info("Serializing edges...")
      SQL(s"SELECT * from ${EdgesTable}")().par.foreach { row =>
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

    def writebackInferenceResult(variableSchema: Map[String, _ <: VariableDataType],
      variableOutputFile: String, weightsOutputFile: String) = {
      log.info("writing back inference result")

      val deserializier = new ProtobufInferenceResultDeserializier()

      // Copy the weight result back to the database
      val weightResultStr = deserializier.getWeights(weightsOutputFile).map { w =>
        s"${w.weightId}\t${w.value}"
      }.mkString("\n")
      PostgresDataStore.copyBatchData(s"COPY ${WeightResultTable}(id, weight) FROM STDIN",
        new java.io.StringReader(weightResultStr))
      SQL(s"""CREATE INDEX ${WeightResultTable}_idx 
        ON ${WeightResultTable} using btree (weight);""").execute()

      // Copy the variable result back into the database
      val variableResultStr = deserializier.getVariables(variableOutputFile).map { v =>
        s"${v.variableId}\t${v.category}\t${v.expectation}"
      }.mkString("\n")
       PostgresDataStore.copyBatchData(s"COPY ${VariableResultTable}(id, category, expectation) FROM STDIN",
        new java.io.StringReader(variableResultStr))
      SQL(s"CREATE INDEX ${VariableResultTable}_idx ON ${VariableResultTable} using btree (expectation);").execute()
      SQL("analyze").execute()

      // Create useful indicies
      SQL(s"CREATE INDEX ${FactorsTable}_weight_id_idx ON ${FactorsTable} (weight_id);").execute()
      SQL(s"CREATE INDEX ${EdgesTable}_factor_id_idx ON ${EdgesTable} (factor_id);").execute()
      SQL(s"CREATE INDEX ${EdgesTable}_variable_id_idx ON ${EdgesTable} (variable_id);").execute()

      // Each (relation, column) tuple is a variable in the plate model.
      // Find all (relation, column) combinations
      val relationsColumns = variableSchema.keys map (_.split('.')) map {
        case Array(relation, column) => (relation, column)
      } 

      // TODO: Handle different data types
      // Generate a view for each (relation, column) combination.
      relationsColumns.foreach { case(relationName, columnName) => 
        val view_name = s"${relationName}_${columnName}_inference"
        log.info(s"creating view=${view_name}")
        SQL(s"""CREATE OR REPLACE VIEW ${view_name} AS
          SELECT ${relationName}.*, mir.category, mir.expectation FROM
          ${relationName} JOIN
            (SELECT mir.category, mir.expectation, mir.id, mir.mid 
            FROM ${MappedInferenceResultView} mir 
            WHERE mrel = '${relationName}' AND mcol = '${columnName}'
            ORDER BY mir.expectation DESC) 
          mir ON ${relationName}.id = mir.mid""").execute()
      }
      
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
            INNER JOIN ${LocalVariableMapTable} ON ${LocalVariableMapTable}.id = ${VariablesTable}.id
            WHERE ${LocalVariableMapTable}.mrel = '${relationName}' 
              AND ${LocalVariableMapTable}.mcol = '${columnName}'),
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

    def getCalibrationData(variable: String, dataType: VariableDataType, buckets: List[Bucket]) : Map[Bucket, BucketData] = {
      val Array(relationName, columnName) = variable.split('.')
      val inference_view = s"${relationName}_${columnName}_inference"
      val bucketed_view = s"${relationName}_${columnName}_inference_bucketed"
      val calibration_view = s"${relationName}_${columnName}_calibration"
      
      // Generate a temporary bucketed view
      val bucketCaseStatement = buckets.zipWithIndex.map { case(bucket, index) =>
        s"when expectation >= ${bucket.from} and expectation <= ${bucket.to} then ${index}"
      }.mkString("\n")
      SQL(s"""create or replace view ${bucketed_view} AS
        SELECT ${inference_view}.*, case ${bucketCaseStatement} end bucket
        FROM ${inference_view} ORDER BY bucket ASC""").execute()
      log.info(s"created bucketed_inference_view=${bucketed_view}")

      // Generate the calibration view
      val calibrationViewQuery = dataType match {
        case BooleanType => 
          s"""create or replace view ${calibration_view} AS
            SELECT b1.bucket, b1.numVariables, b2.numCorrect, b3.numIncorrect FROM
            (SELECT bucket, COUNT(*) AS numVariables from ${bucketed_view} GROUP BY bucket) b1
            LEFT JOIN (SELECT bucket, COUNT(*) AS numCorrect from ${bucketed_view} 
              WHERE ${columnName}=true GROUP BY bucket) b2 ON b1.bucket = b2.bucket
            LEFT JOIN (SELECT bucket, COUNT(*) AS numIncorrect from ${bucketed_view} 
              WHERE ${columnName}=false GROUP BY bucket) b3 ON b1.bucket = b3.bucket 
          ORDER BY b1.bucket ASC"""
        case MultinomialType(_) =>
          s"""create or replace view ${calibration_view} AS
            SELECT b1.bucket, b1.numVariables, b2.numCorrect, b3.numIncorrect FROM
            (SELECT bucket, COUNT(*) AS numVariables from ${bucketed_view} GROUP BY bucket) b1
            LEFT JOIN (SELECT bucket, COUNT(*) AS numCorrect from ${bucketed_view} 
              WHERE ${columnName} = category GROUP BY bucket) b2 ON b1.bucket = b2.bucket
            LEFT JOIN (SELECT bucket, COUNT(*) AS numIncorrect from ${bucketed_view} 
              WHERE ${columnName} != category GROUP BY bucket) b3 ON b1.bucket = b3.bucket 
          ORDER BY b1.bucket ASC"""

      }
      SQL(calibrationViewQuery).execute()

      log.info(s"created calibration_view=${calibration_view}")

      // Build and Return the final calibration data
      val bucketData = SQL(s"SELECT * from ${calibration_view}")().map { row =>
        val bucket = row[Long]("bucket")
        val data = BucketData(row[Option[Long]]("numVariables").getOrElse(0),
          row[Option[Long]]("numCorrect").getOrElse(0), 
          row[Option[Long]]("numIncorrect").getOrElse(0))
        (bucket, data)
      }.toMap

      buckets.zipWithIndex.map { case (bucket, index) =>
        (bucket, bucketData.get(index).getOrElse(BucketData(0,0,0)))
      }.toMap
    }

  }
}
