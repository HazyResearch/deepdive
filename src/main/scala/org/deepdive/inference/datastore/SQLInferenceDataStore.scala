package org.deepdive.inference

import java.io.{File, PrintWriter}
import org.deepdive.calibration._
import org.deepdive.datastore.JdbcDataStore
import org.deepdive.Logging
import org.deepdive.settings._
import scalikejdbc._
import scala.util.matching._
import scala.io.Source

/* Stores the factor graph and inference results. */
trait SQLInferenceDataStoreComponent extends InferenceDataStoreComponent {

  def inferenceDataStore : SQLInferenceDataStore

}

trait SQLInferenceDataStore extends InferenceDataStore with Logging {

  def ds : JdbcDataStore

  val factorOffset = new java.util.concurrent.atomic.AtomicLong(0)

  /* Internal Table names */
  def WeightsTable = "dd_graph_weights"
  def FactorsTable = "dd_graph_factors"
  def VariablesTable = "dd_graph_variables"
  def VariablesHoldoutTable = "dd_graph_variables_holdout"
  def VariablesMapTable = "dd_graph_variables_map"
  def LocalVariableMapTable = "dd_graph_local_variable_map"
  def EdgesTable = "dd_graph_edges"
  def WeightResultTable = "dd_inference_result_weights"
  def VariableResultTable = "dd_inference_result_variables"
  def MappedInferenceResultView = "dd_mapped_inference_result"

  /* Executes an arbitary SQL statement */
  def execute(sql: String) = ds.DB.autoCommit { implicit session =>
    """;\s+""".r.split(sql.trim()).filterNot(_.isEmpty).map(_.trim).foreach { query => 
      SQL(query).execute.apply()
    }
  }

  /* Issues a query */
  def selectForeach(sql: String)(op: (WrappedResultSet) => Unit) = {
    ds.DB.readOnly { implicit session =>
      SQL(sql).foreach(op)
    }
  }

  def selectAsMap(sql: String) : List[Map[String, Any]] = {
    ds.DB.readOnly { implicit session =>
      SQL(sql).map(_.toMap).list.apply()
    }
  }

  def keyType = "bigserial"
  def stringType = "text"

  def randomFunc = "RANDOM()"

  def createWeightsSQL = s"""
    DROP TABLE IF EXISTS ${WeightsTable} CASCADE;
    CREATE TABLE ${WeightsTable}(
      id ${keyType} primary key,
      initial_value double precision,
      is_fixed boolean,
      description ${stringType});
  """

  def createFactorsSQL = s"""
    DROP TABLE IF EXISTS ${FactorsTable} CASCADE; 
    CREATE TABLE ${FactorsTable}(
      id ${keyType} primary key, 
      weight_id bigint, 
      factor_function ${stringType}, 
      factor_group ${stringType}, 
      qid bigint);
  """
  // CREATE INDEX ${FactorsTable}_id_idx ON ${FactorsTable}(id);
  // CREATE INDEX ${FactorsTable}_qid_idx ON ${FactorsTable}(qid);

  def createVariablesSQL = s"""
    DROP TABLE IF EXISTS ${VariablesTable} CASCADE; 
    CREATE TABLE ${VariablesTable}(
      id ${keyType} primary key, 
      data_type ${stringType},
      initial_value double precision, 
      is_evidence boolean);
  """

  def createVariablesHoldoutSQL = s"""
    DROP TABLE IF EXISTS ${VariablesHoldoutTable} CASCADE; 
    CREATE TABLE ${VariablesHoldoutTable}(
      variable_id bigint primary key);
  """

  def createVariablesMapSQL = s"""
    DROP TABLE IF EXISTS ${VariablesMapTable} CASCADE;
    CREATE TABLE ${VariablesMapTable}(
      id ${keyType},
      variable_id bigint);
    CREATE INDEX ${VariablesMapTable}_variable_id_idx ON ${VariablesMapTable}(variable_id);
  """

  // def createVariablesMapSQL = s"""
  //   DROP TABLE IF EXISTS ${LocalVariableMapTable} CASCADE;
  //   CREATE TABLE ${LocalVariableMapTable}(
  //     id ${keyType},
  //     mrel ${stringType},
  //     mcol ${stringType},
  //     mid bigint,
  //     CONSTRAINT c_pkey UNIQUE (mrel, mcol, mid));
  //   CREATE INDEX ${LocalVariableMapTable}_id_idx ON ${LocalVariableMapTable}(id);
  // """
  // CREATE INDEX ${LocalVariableMapTable}_id_all_idx ON ${LocalVariableMapTable}(mrel, mcol, mid);

  def alterSequencesSQL = s"""
    ALTER SEQUENCE ${WeightsTable}_id_seq MINVALUE -1 RESTART WITH 0;
    ALTER SEQUENCE ${FactorsTable}_id_seq MINVALUE -1 RESTART WITH 0;
    ALTER SEQUENCE ${VariablesTable}_id_seq MINVALUE -1 RESTART WITH 0;
    ALTER SEQUENCE ${VariablesMapTable}_id_seq MINVALUE -1 RESTART WITH 0;
    ALTER SEQUENCE ${LocalVariableMapTable}_id_seq MINVALUE -1 RESTART WITH 0;
  """

  def creatEdgesSQL = s"""
    DROP TABLE IF EXISTS ${EdgesTable}; 
    CREATE TABLE ${EdgesTable}(
      factor_id bigint, 
      variable_id bigint, 
      position int, 
      is_positive boolean);
  """

  def createInferenceResultSQL = s"""
    DROP TABLE IF EXISTS ${VariableResultTable} CASCADE; 
    CREATE TABLE ${VariableResultTable}(
      id bigint, 
      category bigint, 
      expectation double precision);
  """

  def createInferenceResultWeightsSQL = s"""
    DROP TABLE IF EXISTS ${WeightResultTable} CASCADE; 
    CREATE TABLE ${WeightResultTable}(
      id bigint primary key, 
      weight double precision);
  """

  def createMappedInferenceResultView = s"""
    CREATE VIEW ${MappedInferenceResultView} 
    AS SELECT ${VariablesTable}.*, ${VariableResultTable}.category, ${VariableResultTable}.expectation 
    FROM ${VariablesTable}, ${VariablesMapTable}, ${VariableResultTable}
    WHERE ${VariablesTable}.id = ${VariablesMapTable}.variable_id
      AND ${VariablesMapTable}.variable_id = ${VariableResultTable}.id;
  """

  def selectWeightsForDumpSQL = s"""
    SELECT id AS "id", is_fixed AS "is_fixed", initial_value AS "initial_value", 
      description AS "description"
    FROM ${WeightsTable} ORDER BY ID ASC;
  """

  def selectVariablesForDumpSQL(relation: String, attribute: String) = s"""
    SELECT ${VariablesMapTable}.id AS "id", ${VariablesTable}.is_evidence AS "is_evidence",
      ${VariablesTable}.initial_value AS "initial_value", "edge_count"
    FROM ${VariablesTable}, ${VariablesMapTable},
    (SELECT variable_id AS "edges.vid", 
      COUNT(*) as "edge_count" 
      FROM ${EdgesTable} GROUP BY variable_id) tmp
    WHERE ${VariablesTable}.id = ${VariablesMapTable}.variable_id
      AND ${VariablesTable}.id = "edges.vid"
    ORDER BY ${VariablesMapTable}.id ASC;
  """

  def selectFactorsForDumpSQL = s"""
    SELECT id AS "id", weight_id AS "weight_id", factor_function AS "factor_function", "edge_count"
    FROM ${FactorsTable},
    (SELECT factor_id AS "edges.fid", 
      COUNT(*) as edge_count 
      FROM ${EdgesTable} GROUP BY factor_id) tmp
    WHERE id = "edges.fid"
    ORDER BY ID ASC;
  """

  def selectEdgesForDumpSQL = s"""
    SELECT ${VariablesMapTable}.id AS "variable_id", factor_id AS "factor_id", position AS "position",
      is_positive AS "is_positive"
    FROM ${EdgesTable}, ${VariablesMapTable}
    WHERE ${VariablesMapTable}.variable_id = ${EdgesTable}.variable_id
    ORDER BY ${VariablesMapTable}.id ASC;
  """

  def selectMetaDataForDumpSQL = s"""
    SELECT 
      (SELECT COUNT(*) from ${WeightsTable}) num_weights,
      (SELECT COUNT(*) from ${VariablesTable}) num_variables,
      (SELECT COUNT(*) from ${FactorsTable}) num_factors,
      (SELECT COUNT(*) from ${EdgesTable}) num_edges;
  """

  // def materializeQuerySQL(name: String, query: String, weightVariables: Seq[String], weightPrefix: String) = {
  //   // Command for generating the weight string
  //   val weightCmd = weightVariables.map ( v => s""" "${v}" """ ).mkString(", ") match { 
  //     case "" => "''"
  //     case x => s"""concat_ws(',','${weightPrefix}', ${x})"""
  //   }
  //   s"""
  //   DROP VIEW IF EXISTS ${name}_tmp CASCADE;
  //   DROP TABLE IF EXISTS ${name} CASCADE;
  //   CREATE VIEW ${name}_tmp AS (${query});
  //   CREATE TABLE ${name} AS 
  //   (SELECT row_number() OVER() - 1 as factor_id,
  //     ${name}_tmp.*, ${weightCmd} AS dd_weight
  //   FROM ${name}_tmp) WITH DATA;
  //   CREATE INDEX ${name}_factor_id_idx ON ${name}(factor_id);
  //   """
  // }
  // // CREATE INDEX ${name}_weight_idx ON ${name}(dd_weight);

  // def groundInsertWeightsSQL(weightValue: Double, is_fixed: Boolean, queryName: String) = s"""
  //   INSERT INTO ${WeightsTable}(description, initial_value, is_fixed)
  //   (SELECT DISTINCT dd_weight::text, ${weightValue}, ${is_fixed} FROM ${queryName});
  // """

  // def groundInsertFactorsSQL(factorFunc: String, factorGroup: String, queryName: String) = s"""
  //   INSERT INTO ${FactorsTable}(weight_id, factor_function, factor_group, qid)
  //   (SELECT ${WeightsTable}.id, '${factorFunc}', '${factorGroup}', factor_id
  //   FROM ${queryName}, ${WeightsTable}
  //   WHERE dd_weight::text = ${WeightsTable}.description);
  // """

  // def groundInsertLocalVariablesSQL(relation: String, valueColumn: String, idColumn: String, 
  //   queryName: String) = s"""
  //   INSERT INTO ${LocalVariableMapTable}(mrel, mcol, mid)
  //   (SELECT DISTINCT '${relation}', '${valueColumn}', "${idColumn}"
  //   FROM ${queryName}
  //   EXCEPT SELECT '${relation}', '${valueColumn}', mid 
  //     FROM ${LocalVariableMapTable}
  //     WHERE mrel='${relation}' AND mcol='${valueColumn}');
  // """

  // def groundRebuildIndiciesSQL = s"""ANALYZE;"""

  // def groundInsertGlobalVariablesSQL(relation: String, valueColumn: String, idColumn: String, 
  //   field: String, dataType: String, holdoutFraction: Double, queryName: String) = s"""
  //   DROP VIEW IF EXISTS ${queryName}_new_variables CASCADE;
  //   CREATE VIEW ${queryName}_new_variables AS (
  //     SELECT DISTINCT ${LocalVariableMapTable}.id AS id, '${dataType}' as data_type, 
  //       (CASE WHEN "${field}" THEN 1.0 ELSE 0.0 END) AS initial_value, ("${field}" IS NOT NULL) AS is_evidence
  //     FROM ${LocalVariableMapTable} LEFT JOIN ${VariablesTable}
  //       ON ${LocalVariableMapTable}.id=${VariablesTable}.id,
  //     ${queryName}
  //     WHERE mrel='${relation}' AND mcol='${valueColumn}' AND mid="${idColumn}" 
  //       AND ${VariablesTable}.id IS NULL);
    
  //   DROP TABLE IF EXISTS ${queryName}_holdout CASCADE;
  //   CREATE TABLE ${queryName}_holdout AS (
  //     SELECT id 
  //     FROM ${queryName}_new_variables 
  //     WHERE ${randomFunc} < ${holdoutFraction} AND is_evidence = true) WITH DATA;
    
  //   INSERT INTO ${VariablesTable}(id, data_type, initial_value, is_evidence)
  //   (SELECT id, data_type, initial_value, is_evidence FROM ${queryName}_new_variables);

  //   UPDATE ${VariablesTable} SET is_evidence=false
  //   WHERE ${VariablesTable}.id IN (SELECT id FROM ${queryName}_holdout)
  // """

  // // def groundInsertGlobalVariablesSQL(relation: String, valueColumn: String, idColumn: String, 
  // //   field: String, dataType: String, holdoutFraction: Double, queryName: String) = s"""
  // //   INSERT INTO ${VariablesTable}(id, data_type, initial_value, is_evidence)
  // //   (SELECT ${LocalVariableMapTable}.id, '${dataType}', 
  // //     (CASE WHEN every("${field}") = true THEN 1.0 ELSE 0.0 END),
  // //     (CASE WHEN (COUNT("${field}") = 0 OR ${randomFunc} < ${holdoutFraction}) THEN false ELSE true END)
  // //   FROM 
  // //     ${LocalVariableMapTable} INNER JOIN 
  // //       (SELECT max(id) AS id from ${LocalVariableMapTable} group by id) tmp
  // //     ON ${LocalVariableMapTable}.id = tmp.id
  // //     LEFT JOIN ${VariablesTable} 
  // //       ON ${LocalVariableMapTable}.id=${VariablesTable}.id,
  // //     ${queryName}
  // //   WHERE mrel='${relation}' 
  // //     AND mcol='${valueColumn}'
  // //     AND mid="${idColumn}"
  // //     AND ${VariablesTable}.id IS NULL
  // //   GROUP BY ${LocalVariableMapTable}.id);
  // // """

  // def groundInsertEdgesSQL(relation: String, valueColumn: String, idColumn: String, factorGroup: String,
  //   position: Long, isNegated: Boolean, queryName: String) = s"""
  //    INSERT INTO ${EdgesTable}(factor_id, variable_id, position, is_positive)
  //     (SELECT ${FactorsTable}.id, ${LocalVariableMapTable}.id, ${position}, ${!isNegated}
  //     FROM ${queryName}, ${FactorsTable}, ${LocalVariableMapTable}
  //     WHERE ${queryName}.factor_id = ${FactorsTable}.qid
  //       AND ${LocalVariableMapTable}.mrel = '${relation}'
  //       AND ${LocalVariableMapTable}.mcol = '${valueColumn}'
  //       AND ${LocalVariableMapTable}.mid = "${idColumn}"
  //       AND ${FactorsTable}.factor_group = '${factorGroup}')
  // """

  def createInferenceResultIndiciesSQL = s"""
    CREATE INDEX ${WeightResultTable}_idx ON ${WeightResultTable} (weight);
    CREATE INDEX ${VariableResultTable}_idx ON ${VariableResultTable} (expectation);
    CREATE INDEX ${FactorsTable}_weight_id_idx ON ${FactorsTable} (weight_id);
    CREATE INDEX ${EdgesTable}_factor_id_idx ON ${EdgesTable} (factor_id);
    CREATE INDEX ${EdgesTable}_variable_id_idx ON ${EdgesTable} (variable_id);
  """

  def createInferenceViewSQL(relationName: String, columnName: String) = s"""
    CREATE VIEW ${relationName}_${columnName}_inference AS
    (SELECT ${relationName}.*, mir.category, mir.expectation FROM
    ${relationName}, ${MappedInferenceResultView} mir
    WHERE ${relationName}.id = mir.id
    ORDER BY mir.expectation DESC);
  """

  def createMappedWeightsViewSQL = s"""
    CREATE VIEW ${VariableResultTable}_mapped_weights AS
    SELECT ${WeightsTable}.*, ${WeightResultTable}.weight FROM
    ${WeightsTable} JOIN ${WeightResultTable} ON ${WeightsTable}.id = ${WeightResultTable}.id
    ORDER BY abs(weight) DESC;
  """

  def createVariableWeightsViewSQL(relationName: String, columnName: String) = s"""
    CREATE VIEW top_weights_${relationName}_${columnName} AS (SELECT id
    FROM ${VariableResultTable}_mapped_weights 
    ORDER BY abs(weight) DESC);
    
    CREATE VIEW relevant_variables_${relationName}_${columnName} AS
    (SELECT top_weights_${relationName}_${columnName}.id, is_evidence, initial_value FROM top_weights_${relationName}_${columnName} 
    INNER JOIN ${FactorsTable} ON ${FactorsTable}.weight_id = top_weights_${relationName}_${columnName}.id 
    INNER JOIN ${EdgesTable} ON ${FactorsTable}.id = ${EdgesTable}.factor_id 
    INNER JOIN ${VariablesTable} ON ${EdgesTable}.variable_id = ${VariablesTable}.id
    INNER JOIN ${LocalVariableMapTable} ON ${LocalVariableMapTable}.id = ${VariablesTable}.id
    WHERE ${LocalVariableMapTable}.mrel = '${relationName}' 
      AND ${LocalVariableMapTable}.mcol = '${columnName}');

    CREATE VIEW grouped_weights__${relationName}_${columnName} AS
    (SELECT relevant_variables_${relationName}_${columnName}.id,
      COUNT(CASE WHEN relevant_variables_${relationName}_${columnName}.is_evidence=true 
        AND relevant_variables_${relationName}_${columnName}.initial_value=1.0 THEN 1 END) AS true_count,
      COUNT(CASE WHEN relevant_variables_${relationName}_${columnName}.is_evidence=true 
        AND relevant_variables_${relationName}_${columnName}.initial_value=0.0 THEN 1 END) AS false_count,
      COUNT(CASE WHEN relevant_variables_${relationName}_${columnName}.is_evidence=false THEN 1 END) AS total_count
      FROM relevant_variables_${relationName}_${columnName} GROUP BY id);
    
    CREATE VIEW ${relationName}_${columnName}_weights AS
    (SELECT description, weight, true_count, false_count, total_count
      FROM grouped_weights__${relationName}_${columnName} INNER JOIN ${VariableResultTable}_mapped_weights 
        ON grouped_weights__${relationName}_${columnName}.id = ${VariableResultTable}_mapped_weights.id
      ORDER BY abs(weight) DESC);
    """

  def createBucketedCalibrationViewSQL(name: String, inferenceViewName: String, buckets: List[Bucket]) = {
    val bucketCaseStatement = buckets.zipWithIndex.map { case(bucket, index) =>
      s"WHEN expectation >= ${bucket.from} AND expectation <= ${bucket.to} THEN ${index}"
    }.mkString("\n")
    s"""CREATE VIEW ${name} AS
      SELECT ${inferenceViewName}.*, CASE ${bucketCaseStatement} END bucket
      FROM ${inferenceViewName} ORDER BY bucket ASC;"""
  }

  def createCalibrationViewBooleanSQL(name: String, bucketedView: String, columnName: String) =  s"""
    CREATE VIEW ${name} AS
    SELECT b1.bucket, b1.num_variables, b2.num_correct, b3.num_incorrect FROM
    (SELECT bucket, COUNT(*) AS num_variables from ${bucketedView} GROUP BY bucket) b1
    LEFT JOIN (SELECT bucket, COUNT(*) AS num_correct from ${bucketedView} 
      WHERE ${columnName}=true GROUP BY bucket) b2 ON b1.bucket = b2.bucket
    LEFT JOIN (SELECT bucket, COUNT(*) AS num_incorrect from ${bucketedView} 
      WHERE ${columnName}=false GROUP BY bucket) b3 ON b1.bucket = b3.bucket 
    ORDER BY b1.bucket ASC;
  """

  def createCalibrationViewMultinomialSQL(name: String, bucketedView: String, columnName: String) =  s"""
    CREATE VIEW ${name} AS
    SELECT b1.bucket, b1.num_variables, b2.num_correct, b3.num_incorrect FROM
    (SELECT bucket, COUNT(*) AS num_variables from ${bucketedView} GROUP BY bucket) b1
    LEFT JOIN (SELECT bucket, COUNT(*) AS num_correct from ${bucketedView} 
      WHERE ${columnName} = category GROUP BY bucket) b2 ON b1.bucket = b2.bucket
    LEFT JOIN (SELECT bucket, COUNT(*) AS num_incorrect from ${bucketedView} 
      WHERE ${columnName} != category GROUP BY bucket) b3 ON b1.bucket = b3.bucket 
    ORDER BY b1.bucket ASC;
  """

  def selectCalibrationDataSQL(name: String) = s"""
    SELECT bucket as "bucket", num_variables AS "num_variables", 
      num_correct AS "num_correct", num_incorrect AS "num_incorrect"
    FROM ${name};
  """


  def init() : Unit = {
    // execute(createWeightsSQL)
    // execute(createFactorsSQL)
    // execute(createVariablesSQL)
    // execute(createVariablesHoldoutSQL)
    // execute(createVariablesMapSQL)
    // execute(creatEdgesSQL)
    // execute(createInferenceResultSQL)
    // execute(createInferenceResultWeightsSQL)
    // execute(createMappedInferenceResultView)
    // execute(alterSequencesSQL)
  }

  def dumpFactorGraph(serializer: Serializer, schema: Map[String, _ <: VariableDataType],
    weightsPath: String, variablesPath: String, factorsPath: String, edgesPath: String) : Unit = {
    log.info(s"Dumping factor graph...")
    log.info("Serializing weights...")
    selectForeach(selectWeightsForDumpSQL) { rs => 
      serializer.addWeight(rs.long("id"), rs.boolean("is_fixed"), 
        rs.double("initial_value"), rs.string("description"))
    }
    schema.foreach { case(relationField, dataType) => 
      val Array(relationName, columnName) = relationField.split('.')
      log.info(s"""Serializing variable=" ${relationField} " """)
      selectForeach(selectVariablesForDumpSQL(relationName, columnName)) { rs => 
        serializer.addVariable(
          rs.long("id"),
          if (rs.boolean("is_evidence")) rs.doubleOpt("initial_value") else None,
          dataType, 
          rs.long("edge_count"),
          dataType.cardinality)
      }
    }
    log.info("Serializing factors...")
    selectForeach(selectFactorsForDumpSQL) { rs => 
      serializer.addFactor(rs.long("id"), rs.long("weight_id"),
        rs.string("factor_function"), rs.long("edge_count"))
    }
    log.info("Serializing edges...")
    selectForeach(selectEdgesForDumpSQL) { rs => 
      serializer.addEdge(rs.long("variable_id"), rs.long("factor_id"),
        rs.long("position"), rs.boolean("is_positive"))
    }

    selectForeach(selectMetaDataForDumpSQL) { rs =>
      serializer.writeMetadata(
        rs.long("num_weights"), rs.long("num_variables"), rs.long("num_factors"), rs.long("num_edges"),
        weightsPath, variablesPath, factorsPath, edgesPath)
    }


    log.info("Writing serialization result...")
    serializer.close()
  }

  def groundFactorGraph(schema: Map[String, _ <: VariableDataType], factorDescs: Seq[FactorDesc],
    holdoutFraction: Double) {

    // We write the grounding queries to this SQL file
    val sqlFile = File.createTempFile(s"grounding", ".sql")
    val writer = new PrintWriter(sqlFile)
    log.info(s"""Writing commands to file="${sqlFile}" """)

    writer.println(createWeightsSQL)
    writer.println(createFactorsSQL)
    writer.println(createVariablesSQL)
    writer.println(createVariablesHoldoutSQL)
    writer.println(createVariablesMapSQL)
    writer.println(creatEdgesSQL)
    writer.println(alterSequencesSQL)

    // Ground all variables in the schema
    schema.foreach { case(variable, dataType) =>
      val Array(relation, column) = variable.split('.')
      writer.println(
        s"""INSERT INTO ${VariablesTable}(id, data_type, initial_value, is_evidence)
        SELECT ${relation}.id, '${dataType}', ${variable}::int, (${variable} IS NOT NULL)
        FROM ${relation};""")
    }

    // Map variables to sequential IDs
    writer.println(
      s"""INSERT INTO ${VariablesMapTable}(variable_id)
      SELECT id FROM ${VariablesTable};""")

    // Assign the holdout
    writer.println(s"""INSERT INTO ${VariablesHoldoutTable}(variable_id)
      SELECT id FROM ${VariablesTable}
      WHERE ${randomFunc} < ${holdoutFraction} AND is_evidence = true;""")
    writer.println(s"""UPDATE ${VariablesTable} SET is_evidence=false
      WHERE ${VariablesTable}.id IN (SELECT variable_id FROM ${VariablesHoldoutTable});""")

    // Create views for each inference rule
    factorDescs.zipWithIndex.foreach { case(factorDesc, i) =>

      val factorOffsetCmd = i match {
        case 0 => "0"
        case x => s"""(SELECT max(factor_id)+1 FROM ${factorDescs(i-1).name}_query)"""
      }

      writer.println(s"""
        DROP VIEW IF EXISTS ${factorDesc.name}_query_user CASCADE;
        CREATE VIEW ${factorDesc.name}_query_user AS ${factorDesc.inputQuery};""")
      writer.println(s"""
        DROP MATERIALIZED VIEW IF EXISTS ${factorDesc.name}_query CASCADE;
        CREATE MATERIALIZED VIEW ${factorDesc.name}_query AS 
        SELECT 
          row_number() OVER() - 1 + ${factorOffsetCmd} as factor_id,
          ${factorDesc.name}_query_user.*
        FROM ${factorDesc.name}_query_user;""")
      
    }

    // Ground weights for each inference rule
    factorDescs.foreach { factorDesc =>
      val weightValue = factorDesc.weight match { 
        case x : KnownFactorWeight => x.value
        case _ => 0.0
      }
      val isFixed = factorDesc.weight.isInstanceOf[KnownFactorWeight]
      val weightPrefix = factorDesc.weightPrefix
      val weightCmd = factorDesc.weight.variables.map ( v => s""" "${v}" """ ).mkString(", ") match { 
        case "" => weightPrefix
        case x => s"""concat_ws('-','${weightPrefix}', ${x})"""
      }

      writer.println(s"""
        INSERT INTO ${WeightsTable}(initial_value, is_fixed, description)
        SELECT DISTINCT ${weightValue}, ${isFixed}, ${weightCmd}
        FROM ${factorDesc.name}_query;""")
    }
    // Add index to the weights
    writer.println(s"""CREATE INDEX ${WeightsTable}_desc_idx ON ${WeightsTable}(description);""")
    writer.println(s"""ANALYZE ${WeightsTable};""")


    // Ground all factors
    factorDescs.foreach { factorDesc =>
      val weightPrefix = factorDesc.weightPrefix
      val weightCmd = factorDesc.weight.variables.map ( v => s""" "${v}" """ ).mkString(", ") match { 
        case "" => weightPrefix
        case x => s"""concat_ws('-','${weightPrefix}', ${x})"""
      }
      val functionName = factorDesc.func.getClass.getSimpleName
      writer.println(s"""
        INSERT INTO ${FactorsTable}(id, weight_id, factor_function, factor_group)
        SELECT factor_id, ${WeightsTable}.id, '${functionName}', '${factorDesc.name}'
        FROM ${factorDesc.name}_query, ${WeightsTable}
        WHERE ${weightCmd} = ${WeightsTable}.description;""")
    }

    // Ground all edges
    factorDescs.foreach { factorDesc =>
      factorDesc.func.variables.zipWithIndex.foreach { case(variable, position) =>
        val vRelation = variable.headRelation
        val vColumn = variable.field
        val vidColumn = s"${variable.relation}.id"
        val isPositive = !variable.isNegated
        writer.println(s"""
          INSERT INTO ${EdgesTable}(factor_id, variable_id, position, is_positive)
          SELECT factor_id, "${vidColumn}", ${position}, ${isPositive}
          FROM ${factorDesc.name}_query;""")
      }
    }

    writer.close()

    log.info("Executing grounding query...")
    execute(Source.fromFile(sqlFile).getLines.mkString)


    // val queryName = s"dd_${factorDesc.name}_query"
    // val weightValue = factorDesc.weight match { 
    //   case x : KnownFactorWeight => x.value
    //   case _ => 0.0
    // }
    // log.info("Grounding rule query...")
    // execute(materializeQuerySQL(queryName, factorDesc.inputQuery, factorDesc.weight.variables, factorDesc.weightPrefix))
    // log.info("Inserting weights...")
    // execute(groundInsertWeightsSQL(weightValue, factorDesc.weight.isInstanceOf[KnownFactorWeight], queryName))
    
    // factorDesc.func.variables.zipWithIndex.foreach { case(variable, position) =>
    //   val vRelation = variable.headRelation
    //   val vColumn = variable.field
    //   val vidColumn = s"${variable.relation}.id"
    //   val variableDataType = factorDesc.func.variableDataType
    //   log.info(s"""Inserting local variable="${variable.toString}"...""")
    //   execute(groundInsertLocalVariablesSQL(vRelation, vColumn, vidColumn, queryName))
    // }

    // log.info("Rebuilding indicies...")
    // execute(groundRebuildIndiciesSQL)
    
    // log.info("Inserting factors...")
    // execute(groundInsertFactorsSQL(factorDesc.func.getClass.getSimpleName, factorDesc.name, queryName))
    
    // factorDesc.func.variables.zipWithIndex.foreach { case(variable, position) =>
    //   val vRelation = variable.headRelation
    //   val vColumn = variable.field
    //   val vidColumn = s"${variable.relation}.id"
    //   val variableDataType = factorDesc.func.variableDataType
    //   log.info(s"""Inserting global variable="${variable.toString}"...""")
    //   execute(groundInsertGlobalVariablesSQL(vRelation, vColumn, vidColumn, 
    //     variable.toString, variableDataType, holdoutFraction, queryName))
    //   log.info(s"""Inserting edges for variable="${variable.toString}"...""")
    //   execute(groundInsertEdgesSQL(vRelation, vColumn, vidColumn, factorDesc.name,
    //     position, variable.isNegated, queryName))
    // }
  }

  def bulkCopyWeights(weightsFile: String) : Unit
  def bulkCopyVariables(variablesFile: String) : Unit

  def writebackInferenceResult(variableSchema: Map[String, _ <: VariableDataType],
    variableOutputFile: String, weightsOutputFile: String) = {

    execute(createInferenceResultSQL)
    execute(createInferenceResultWeightsSQL)
    execute(createMappedInferenceResultView)

    log.info("Copying inference result weights...")
    bulkCopyWeights(weightsOutputFile)
    log.info("Copying inference result variables...")
    bulkCopyVariables(variableOutputFile)
    log.info("Creating indicies on the inference result...")
    execute(createInferenceResultIndiciesSQL)

    // Each (relation, column) tuple is a variable in the plate model.
    // Find all (relation, column) combinations
    val relationsColumns = variableSchema.keys map (_.split('.')) map {
      case Array(relation, column) => (relation, column)
    } 

    execute(createMappedWeightsViewSQL)

    relationsColumns.foreach { case(relationName, columnName) => 
      execute(createInferenceViewSQL(relationName, columnName))
      execute(createVariableWeightsViewSQL(relationName, columnName))
    }
  }

  def getCalibrationData(variable: String, dataType: VariableDataType, 
    buckets: List[Bucket]) : Map[Bucket, BucketData] = {

    val Array(relationName, columnName) = variable.split('.')
    val inferenceViewName = s"${relationName}_${columnName}_inference"
    val bucketedViewName = s"${relationName}_${columnName}_inference_bucketed"
    val calibrationViewName = s"${relationName}_${columnName}_calibration"

    execute(createBucketedCalibrationViewSQL(bucketedViewName, inferenceViewName, buckets))
    log.info(s"created calibration_view=${calibrationViewName}")
    dataType match {
      case BooleanType => 
        execute(createCalibrationViewBooleanSQL(calibrationViewName, bucketedViewName, columnName))
      case MultinomialType(_) =>
        execute(createCalibrationViewMultinomialSQL(calibrationViewName, bucketedViewName, columnName))
    }
    
    val bucketData = selectAsMap(selectCalibrationDataSQL(calibrationViewName)).map { row =>
      val bucket = row("bucket")
      val data = BucketData(
        row.get("num_variables").map(_.asInstanceOf[Long]).getOrElse(0), 
        row.get("num_correct").map(_.asInstanceOf[Long]).getOrElse(0), 
        row.get("num_incorrect").map(_.asInstanceOf[Long]).getOrElse(0))
      (bucket, data)
    }.toMap
    buckets.zipWithIndex.map { case (bucket, index) =>
      (bucket, bucketData.get(index).getOrElse(BucketData(0,0,0)))
    }.toMap
  }




}