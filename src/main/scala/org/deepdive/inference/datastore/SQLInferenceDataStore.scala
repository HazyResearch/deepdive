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
  def issueQuery(sql: String)(op: (java.sql.ResultSet) => Unit) = {

    val conn = ds.borrowConnection()
    conn.setAutoCommit(false);
    val stmt = conn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
      java.sql.ResultSet.CONCUR_READ_ONLY);
    stmt.setFetchSize(10000);
    val rs = stmt.executeQuery(sql)
    while(rs.next()){
      op(rs)
    }
    conn.close()
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
      cardinality bigint, 
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

  def alterSequencesSQL = s"""
    ALTER SEQUENCE ${WeightsTable}_id_seq MINVALUE -1 RESTART WITH 0;
    ALTER SEQUENCE ${FactorsTable}_id_seq MINVALUE -1 RESTART WITH 0;
    ALTER SEQUENCE ${VariablesTable}_id_seq MINVALUE -1 RESTART WITH 0;
    ALTER SEQUENCE ${VariablesMapTable}_id_seq MINVALUE -1 RESTART WITH 0;
  """

  def createEdgesSQL = s"""
    DROP TABLE IF EXISTS ${EdgesTable}; 
    CREATE TABLE ${EdgesTable}(
      factor_id bigint, 
      variable_id bigint, 
      position int, 
      is_positive boolean,
      equal_predicate bigint);
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
      AND ${VariablesMapTable}.id = ${VariableResultTable}.id;
  """

  def selectWeightsForDumpSQL_RAW = s"""
    DROP TABLE IF EXISTS selectWeightsForDumpSQL_RAW;
    CREATE TABLE selectWeightsForDumpSQL_RAW AS
    SELECT id AS "id", is_fixed AS "is_fixed", initial_value AS "initial_value", 
      description AS "description"
    FROM ${WeightsTable} ORDER BY ID ASC;
  """

  def selectVariablesForDumpSQL_RAW = s"""
    DROP TABLE IF EXISTS selectVariablesForDumpSQL_RAW;
    CREATE TABLE selectVariablesForDumpSQL_RAW AS
    SELECT ${VariablesMapTable}.id AS "id", is_evidence, data_type, initial_value, edge_count, cardinality
    FROM ${VariablesTable} INNER JOIN ${VariablesMapTable}
      ON ${VariablesTable}.id = ${VariablesMapTable}.variable_id
    LEFT JOIN
    (SELECT variable_id AS "edges.vid", 
      COUNT(*) as "edge_count" 
      FROM ${EdgesTable} GROUP BY variable_id) tmp 
    ON ${VariablesTable}.id = "edges.vid"
    ORDER BY ${VariablesMapTable}.id ASC;
  """

  def selectFactorsForDumpSQL_RAW = s"""
    DROP TABLE IF EXISTS selectFactorsForDumpSQL_RAW;
    CREATE TABLE selectFactorsForDumpSQL_RAW AS
    SELECT id AS "id", weight_id AS "weight_id", factor_function AS "factor_function", "edge_count"
    FROM ${FactorsTable},
    (SELECT factor_id AS "edges.fid", 
      COUNT(*) as edge_count 
      FROM ${EdgesTable} GROUP BY factor_id) tmp
    WHERE id = "edges.fid"
    ORDER BY ID ASC;
  """

  def selectEdgesForDumpSQL_RAW = s"""
    DROP TABLE IF EXISTS selectEdgesForDumpSQL_RAW;
    CREATE TABLE selectEdgesForDumpSQL_RAW AS
    SELECT ${VariablesMapTable}.id AS "variable_id", factor_id, position, is_positive, equal_predicate
    FROM ${EdgesTable}, ${VariablesMapTable}
    WHERE ${VariablesMapTable}.variable_id = ${EdgesTable}.variable_id
    ORDER BY ${VariablesMapTable}.id ASC;
  """

  def selectWeightsForDumpSQL = s"""
    SELECT id AS "id", is_fixed AS "is_fixed", initial_value AS "initial_value", 
      description AS "description"
    FROM selectWeightsForDumpSQL_RAW;
  """

  def selectVariablesForDumpSQL = s"""
    SELECT id AS "id", is_evidence, initial_value, data_type, edge_count, cardinality
    FROM selectVariablesForDumpSQL_RAW;
  """

  def selectFactorsForDumpSQL = s"""
    SELECT id AS "id", weight_id AS "weight_id", factor_function AS "factor_function", "edge_count"
    FROM selectFactorsForDumpSQL_RAW;
  """

  def selectEdgesForDumpSQL = s"""
    SELECT variable_id AS "variable_id", factor_id AS "factor_id", position AS "position", is_positive AS "is_positive", equal_predicate AS "equal_predicate"
    FROM selectEdgesForDumpSQL_RAW;
  """

  def selectMetaDataForDumpSQL = s"""
    SELECT 
      (SELECT COUNT(*) from ${WeightsTable}) num_weights,
      (SELECT COUNT(*) from ${VariablesTable}) num_variables,
      (SELECT COUNT(*) from ${FactorsTable}) num_factors,
      (SELECT COUNT(*) from ${EdgesTable}) num_edges;
  """

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

  // TODO
  def createVariableWeightsViewSQL(relationName: String, columnName: String) = ""

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
    // execute(createEdgesSQL)
    // execute(createInferenceResultSQL)
    // execute(createInferenceResultWeightsSQL)
    // execute(createMappedInferenceResultView)
    // execute(alterSequencesSQL)
  }

  def dumpFactorGraph(serializer: Serializer, schema: Map[String, _ <: VariableDataType],
    weightsPath: String, variablesPath: String, factorsPath: String, edgesPath: String) : Unit = {
    log.info(s"Dumping factor graph...")

    ds.DB.autoCommit { implicit session =>
      SQL(selectFactorsForDumpSQL_RAW).execute.apply()
    }

    ds.DB.autoCommit { implicit session =>
      SQL(selectEdgesForDumpSQL_RAW).execute.apply()
    } 

    ds.DB.autoCommit { implicit session =>
      SQL(selectWeightsForDumpSQL_RAW).execute.apply()
    }

    ds.DB.autoCommit { implicit session =>
      SQL(selectVariablesForDumpSQL_RAW).execute.apply()
    }

    log.info("Serializing weights...")
    issueQuery(selectWeightsForDumpSQL) { rs => 
      serializer.addWeight(rs.getLong(1), rs.getBoolean(2), 
        rs.getDouble(3), rs.getString(4))
    }
    log.info(s"""Serializing variables...""")
    issueQuery(selectVariablesForDumpSQL) { rs => 
      serializer.addVariable(
        rs.getLong(1),
        rs.getBoolean(2),
        rs.getDouble(3), // return 0 if the value is SQL null
        rs.getString(4), 
        rs.getLong(5),
        rs.getLong(6)) // return 0 if ...
    }
    log.info("Serializing factors...")
    selectForeach(selectFactorsForDumpSQL) { rs => 
      serializer.addFactor(rs.long("id"), rs.long("weight_id"),
        rs.string("factor_function"), rs.long("edge_count"))
    }
    log.info("Serializing edges...")
    issueQuery(selectEdgesForDumpSQL) { rs => 
      serializer.addEdge(
        rs.getLong(1),
        rs.getLong(2),
        rs.getLong(3), 
        rs.getBoolean(4),
        rs.getLong(5))
    }

    issueQuery(selectMetaDataForDumpSQL) { rs =>
      serializer.writeMetadata(
        rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getLong(4),
        weightsPath, variablesPath, factorsPath, edgesPath)
    }


    log.info("Writing serialization result...")
    serializer.close()
  }

  def groundFactorGraph(schema: Map[String, _ <: VariableDataType], factorDescs: Seq[FactorDesc],
    holdoutFraction: Double, holdoutQuery: Option[String]) {

    // We write the grounding queries to this SQL file
    val sqlFile = File.createTempFile(s"grounding", ".sql")
    val writer = new PrintWriter(sqlFile)
    log.info(s"""Writing grounding queries to file="${sqlFile}" """)

    writer.println(createWeightsSQL)
    writer.println(createFactorsSQL)
    writer.println(createVariablesSQL)
    writer.println(createVariablesHoldoutSQL)
    writer.println(createVariablesMapSQL)
    writer.println(createEdgesSQL)
    writer.println(alterSequencesSQL)

    // Ground all variables in the schema
    schema.foreach { case(variable, dataType) =>
      val Array(relation, column) = variable.split('.')
      val cardinalityStr = dataType match {
        case BooleanType => "null"
        case MultinomialType(x) => x.toString
      }

      writer.println(
        s"""INSERT INTO ${VariablesTable}(id, data_type, initial_value, is_evidence, cardinality)
        SELECT ${relation}.id, '${dataType}', ${variable}::int, (${variable} IS NOT NULL), ${cardinalityStr}
        FROM ${relation};""")

      // Create a cardinality table for the variable
      val cardinalityValues = dataType match {
        case BooleanType => "(1)"
        case MultinomialType(x) => (0 to x-1).map (x => s"(${x})").mkString(", ")
      }
      val cardinalityTableName = s"${relation}_${column}_cardinality"
      writer.println(s"""
        DROP TABLE IF EXISTS ${cardinalityTableName} CASCADE;""")
      writer.println(
        s"""CREATE TABLE ${cardinalityTableName}(${cardinalityTableName}) AS VALUES ${cardinalityValues} WITH DATA;""")
    }

    // Map variables to sequential IDs
    writer.println(
      s"""INSERT INTO ${VariablesMapTable}(variable_id)
      SELECT id FROM ${VariablesTable};""")

    // Assign the holdout - Random (default) or user-defined query
    holdoutQuery match {
      case None => writer.println(
        s"""INSERT INTO ${VariablesHoldoutTable}(variable_id)
        SELECT id FROM ${VariablesTable}
        WHERE ${randomFunc} < ${holdoutFraction} AND is_evidence = true;""")
      case Some(userQuery) => writer.println(userQuery)
    }
   
    writer.println(s"""UPDATE ${VariablesTable} SET is_evidence=false
      WHERE ${VariablesTable}.id IN (SELECT variable_id FROM ${VariablesHoldoutTable});""")

    writer.println(s"""
       DROP TABLE IF EXISTS factornum CASCADE;""")
    writer.println(s"""
       CREATE TABLE factornum(nfactor bigint);""")
    writer.println(s"""
       INSERT INTO factornum VALUES (0);""")

    // Create views for each inference rule
    factorDescs.zipWithIndex.foreach { case(factorDesc, i) =>

      val factorOffsetCmd = s"""(SELECT sum(nfactor) FROM factornum)"""

      // create cardinality table for each predicate
      factorDesc.func.variables.zipWithIndex.foreach { case(v,idx) => {
        val cardinalityTableName = s"${v.headRelation}_${v.field}_cardinality_${idx}"
        v.predicate match {
          case Some(s) => 
            writer.println(s"""
              DROP TABLE IF EXISTS ${cardinalityTableName} CASCADE;
              CREATE TABLE ${cardinalityTableName}(${v.headRelation}_${v.field}_cardinality) AS VALUES (${s}) WITH DATA;""")
          case None =>
            writer.println(s"""
              DROP VIEW IF EXISTS ${cardinalityTableName} CASCADE;
              CREATE VIEW ${cardinalityTableName} AS SELECT * FROM ${v.headRelation}_${v.field}_cardinality;""")
          }
        }
      }


      val cardinalityTables = factorDesc.func.variables.zipWithIndex.map { case(v,idx) =>
          s"${v.headRelation}_${v.field}_cardinality_${idx} c${idx}"
      }

      val cardinalityValues = factorDesc.func.variables.zipWithIndex.map { case(v,idx) => 
        s"""c${idx}.${v.headRelation}_${v.field}_cardinality AS "${v.relation}_${v.field}_cardinality_${idx}" """
      }

      writer.println(s"""
        DROP VIEW IF EXISTS ${factorDesc.name}_query_user CASCADE;
        CREATE VIEW ${factorDesc.name}_query_user AS ${factorDesc.inputQuery};""")
      writer.println(s"""
        DROP TABLE IF EXISTS ${factorDesc.name}_query CASCADE;
        SELECT
          row_number() OVER() - 1 + ${factorOffsetCmd} as factor_id,
          ${factorDesc.name}_query_user.*,
          ${cardinalityValues.mkString(", ")}
        INTO ${factorDesc.name}_query 
        FROM ${factorDesc.name}_query_user, ${cardinalityTables.mkString(", ")};""")


      writer.println(s"""
        INSERT INTO factornum SELECT count(*) FROM ${factorDesc.name}_query ;""")
      writer.println(s"""
        COMMIT;""")
     
    }

    def generateWeightCmd(weightPrefix: String, weightVariables: Seq[String], cardinalityValues: Seq[String]) : String = 
      weightVariables.map ( v => s"""(CASE WHEN "${v}" IS NULL THEN '' ELSE "${v}"::text END)""" )
        .mkString(" || ") match {
        case "" => s"""'${weightPrefix}-' || ${cardinalityValues.mkString(" || ")} """
        case x => s"""'${weightPrefix}-' || ${x} || ${cardinalityValues.mkString(" || ")}"""
      }

    // Ground weights for each inference rule
    factorDescs.foreach { factorDesc =>
      val weightValue = factorDesc.weight match { 
        case x : KnownFactorWeight => x.value
        case _ => 0.0
      }

      val cardinalityValues = factorDesc.func.variables.zipWithIndex.map { case(v,idx) => 
        s""" "${v.relation}_${v.field}_cardinality_${idx}" """
      }

      val isFixed = factorDesc.weight.isInstanceOf[KnownFactorWeight]
      val weightCmd = generateWeightCmd(factorDesc.weightPrefix, factorDesc.weight.variables,
        cardinalityValues)

      writer.println(s"""
        INSERT INTO ${WeightsTable}(initial_value, is_fixed, description)
        SELECT DISTINCT ${weightValue} AS wValue, ${isFixed} AS wIsFixed, ${weightCmd} AS wCmd
        FROM ${factorDesc.name}_query GROUP BY wValue, wIsFixed, wCmd;""")

    }
    // Add index to the weights
    writer.println(s"""CREATE INDEX ${WeightsTable}_desc_idx ON ${WeightsTable}(description);""")
    writer.println(s"""ANALYZE ${WeightsTable};""")


    // Ground all factors
    factorDescs.foreach { factorDesc =>

      val cardinalityValues = factorDesc.func.variables.zipWithIndex.map { case(v,idx) => 
        s""" "${v.relation}_${v.field}_cardinality_${idx}" """
      }

      val weightCmd = generateWeightCmd(factorDesc.weightPrefix, factorDesc.weight.variables,
        cardinalityValues)
      
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
          INSERT INTO ${EdgesTable}(factor_id, variable_id, position, is_positive, equal_predicate)
          SELECT factor_id, "${vidColumn}", ${position}, ${isPositive}, "${variable.relation}_${vColumn}_cardinality_${position}"
          FROM ${factorDesc.name}_query;""")
      }
    }

    writer.close()

    log.info("Executing grounding query...")
    execute(Source.fromFile(sqlFile).getLines.mkString)
    
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
      // TODO
      // execute(createVariableWeightsViewSQL(relationName, columnName))
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
