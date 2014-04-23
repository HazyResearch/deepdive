package org.deepdive.inference

import java.io.{File, PrintWriter}
import org.deepdive.calibration._
import org.deepdive.datastore.JdbcDataStore
import org.deepdive.Logging
import org.deepdive.settings._
import scalikejdbc._
import scala.util.matching._
import scala.io.Source
// import scala.collection.mutable.Map


/* Stores the factor graph and inference results. */
trait SQLInferenceDataStoreComponent extends InferenceDataStoreComponent {

  def inferenceDataStore : SQLInferenceDataStore

}

trait SQLInferenceDataStore extends InferenceDataStore with Logging {

  def ds : JdbcDataStore

  val factorOffset = new java.util.concurrent.atomic.AtomicLong(0)

  /* Internal Table names */
  def WeightsTable = "dd_graph_weights"
  def lastWeightsTable = "dd_graph_last_weights"
  def FactorsTable = "dd_graph_factors"
  def VariablesTable = "dd_graph_variables"
  def VariablesHoldoutTable = "dd_graph_variables_holdout"
  def VariablesMapTable = "dd_graph_variables_map"
  def EdgesTable = "dd_graph_edges"
  def WeightResultTable = "dd_inference_result_weights"
  def VariableResultTable = "dd_inference_result_variables"
  def MappedInferenceResultView = "dd_mapped_inference_result"
  def IdSequence = "id_sequence"
  def EdgesCountTable = "dd_graph_edges_count"

  /* Executes an arbitary SQL statement */
  def executeSql(sql: String) = ds.DB.autoCommit { implicit session =>
    """;\s+""".r.split(sql.trim()).filterNot(_.isEmpty).map(_.trim).foreach { 
        query => SQL(query).execute.apply()
    }
  }

  def execute(sql: String) {
    try {
      executeSql(sql)
    } catch {
      // SQL cmd exception
      case exception : Throwable =>
        log.error(exception.toString)
        log.info("[Error] Please check the SQL cmd!")
        throw exception
        // TODO: Call TaskManager to kill all tasks
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


  // def copyLastWeightsSQL = s"""
  //   DROP TABLE IF EXISTS ${lastWeightsTable} CASCADE;
  //   SELECT X.*, Y.weight INTO ${lastWeightsTable}
  //     FROM ${WeightsTable} AS X INNER JOIN ${WeightResultTable} AS Y ON X.id = Y.id
  //     ORDER BY id ASC;
  // """

  // def createWeightsSQL = s"""
  //   DROP TABLE IF EXISTS ${WeightsTable} CASCADE;
  //   CREATE TABLE ${WeightsTable}(
  //     id bigserial primary key,
  //     is_fixed boolean,
  //     initial_value double precision,
  //     factor_group int,
  //     description text);
  // """

  // def createFactorsSQL = s"""
  //   DROP TABLE IF EXISTS ${FactorsTable} CASCADE; 
  //   CREATE TABLE ${FactorsTable}( 
  //     variable_ids bigint[],
  //     weight_id bigint, 
  //     variable_negated boolean[],
  //     factor_function ${stringType}, 
  //     factor_group int, 
  //     equal_predicate int);
  // """

  // def createVariablesSQL = s"""
  //   DROP TABLE IF EXISTS ${VariablesTable} CASCADE; 
  //   CREATE TABLE ${VariablesTable}(
  //     id bigserial primary key, 
  //     data_type text,
  //     initial_value double precision, 
  //     cardinality bigint, 
  //     is_evidence boolean);
  // """

  def createVariablesHoldoutSQL = s"""
    DROP TABLE IF EXISTS ${VariablesHoldoutTable} CASCADE; 
    CREATE TABLE ${VariablesHoldoutTable}(
      variable_id bigint primary key);
  """

  // def createVariablesMapSQL = s"""
  //   DROP TABLE IF EXISTS ${VariablesMapTable} CASCADE;
  //   CREATE TABLE ${VariablesMapTable}(
  //     id ${keyType},
  //     variable_id bigint);
  //   CREATE INDEX ${VariablesMapTable}_EdgesCountTablevariable_id_idx ON ${VariablesMapTable}(variable_id);
  // """

  // def alterSequencesSQL = s"""
  //   ALTER SEQUENCE ${WeightsTable}_id_seq MINVALUE -1 RESTART WITH 0;
  //   ALTER SEQUENCE ${VariablesTable}_id_seq MINVALUE -1 RESTART WITH 0;
  //   ALTER SEQUENCE ${VariablesMapTable}_id_seq MINVALUE -1 RESTART WITH 0;
  // """

  def createEdgesSQL = s"""
    DROP TABLE IF EXISTS ${EdgesTable} CASCADE;
    CREATE TABLE ${EdgesTable}(
      variable_id bigint
    );
  """

  def createSequencesSQL = s"""
    DROP SEQUENCE IF EXISTS ${IdSequence};
    CREATE SEQUENCE ${IdSequence} MINVALUE -1 START 0;
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

  // def createMappedInferenceResultView = s"""
  //   CREATE VIEW ${MappedInferenceResultView} 
  //   AS SELECT ${VariablesTable}.*, ${VariableResultTable}.category, ${VariableResultTable}.expectation 
  //   FROM ${VariablesTable}, ${VariablesMapTable}, ${VariableResultTable}
  //   WHERE ${VariablesTable}.id = ${VariablesMapTable}.variable_id
  //     AND ${VariablesMapTable}.id = ${VariableResultTable}.id;
  // """

  // def selectVariablesForDumpSQL_RAW = s"""
  //   DROP TABLE IF EXISTS selectVariablesForDumpSQL_RAW;
  //   CREATE TABLE selectVariablesForDumpSQL_RAW AS
  //   SELECT ${VariablesMapTable}.id AS "id", is_evidence, data_type, initial_value, edge_count, cardinality
  //   FROM ${VariablesTable} INNER JOIN ${VariablesMapTable}
  //     ON ${VariablesTable}.id = ${VariablesMapTable}.variable_id
  //   LEFT JOIN
  //   (SELECT vid as "vid", COUNT(*) AS "edge_count"
  //     FROM (SELECT UNNEST(variable_ids) AS "vid" FROM ${FactorsTable}) tmp
  //     GROUP BY vid) tmp2
  //   ON ${VariablesMapTable}.id = "vid";
  // """

  def selectVariablesMapForDumpSQL = s"""
    SELECT id AS "id", variable_id
    FROM ${VariablesMapTable};
  """

  def selectWeightsForDumpSQL = s"""
    SELECT id AS "id", is_fixed AS "is_fixed", initial_value AS "initial_value"
    FROM ${WeightsTable};
  """

  // def selectVariablesForDumpSQL = s"""
  //   SELECT id AS "id", is_evidence, initial_value, data_type, edge_count, cardinality
  //   FROM selectVariablesForDumpSQL_RAW;
  // """

  // def selectFactorsForDumpSQL = s"""
  //   SELECT weight_id AS "weight_id", variable_ids AS "variable_ids", 
  //     variable_negated as "variable_negated", factor_function AS "factor_function", 
  //     equal_predicate as "equal_predicate"
  //   FROM ${FactorsTable}
  // """

  // def selectMetaDataForDumpSQL = s"""
  //   SELECT 
  //     (SELECT COUNT(*) from ${WeightsTable}) num_weights,
  //     (SELECT COUNT(*) from ${VariablesTable}) num_variables,
  //     (SELECT COUNT(*) from ${FactorsTable}) num_factors,
  //     (SELECT COUNT(*) from (SELECT UNNEST(variable_ids) FROM ${FactorsTable}) as n) num_edges;
  // """

  def createEdgeCountSQL = s"""
    DROP TABLE IF EXISTS ${EdgesCountTable} CASCADE;
    SELECT variable_id, COUNT(*) AS edge_count
    INTO ${EdgesCountTable}
    FROM ${EdgesTable} GROUP BY variable_id;
  """

  // def createInferenceResultIndiciesSQL = s"""
  //   DROP INDEX IF EXISTS ${WeightResultTable}_idx CASCADE;
  //   DROP INDEX IF EXISTS ${VariableResultTable}_idx CASCADE;
  //   DROP INDEX IF EXISTS ${FactorsTable}_weight_id_idx CASCADE;
  //   CREATE INDEX ${WeightResultTable}_idx ON ${WeightResultTable} (weight);
  //   CREATE INDEX ${VariableResultTable}_idx ON ${VariableResultTable} (expectation);
  //   CREATE INDEX ${FactorsTable}_weight_id_idx ON ${FactorsTable} (weight_id);
  // """

  def createInferenceViewSQL(relationName: String, columnName: String) = s"""
    CREATE VIEW ${relationName}_${columnName}_inference AS
    (SELECT ${relationName}.*, mir.category, mir.expectation FROM
    ${relationName}, ${MappedInferenceResultView} mir
    WHERE ${relationName}.id = mir.id
    ORDER BY mir.expectation DESC);
  """

  // def createMappedWeightsViewSQL = s"""
  //   CREATE VIEW ${VariableResultTable}_mapped_weights AS
  //   SELECT ${WeightsTable}.*, ${WeightResultTable}.weight FROM
  //   ${WeightsTable} JOIN ${WeightResultTable} ON ${WeightsTable}.id = ${WeightResultTable}.id
  //   ORDER BY abs(weight) DESC;
  // """

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
  }

  val weightMap = scala.collection.mutable.Map[String, Long]()

  def dumpFactorGraph(serializer: Serializer, schema: Map[String, _ <: VariableDataType],
    factorDescs: Seq[FactorDesc],
    weightsPath: String, variablesPath: String, factorsPath: String, edgesPath: String) : Unit = {

    var numVariables  : Long = 0
    var numFactors    : Long = 0
    var numWeights    : Long = 0
    var numEdges      : Long = 0

    execute(createEdgeCountSQL)
    
    log.info(s"Dumping factor graph...")

    log.info("Dumping variables...")
    schema.foreach { case(variable, dataType) =>
      val Array(relation, column) = variable.split('.')
      val selectVariablesForDumpSQL = s"""
        SELECT id, (${variable} IS NOT NULL), ${variable}::int, edge_count
        FROM ${relation}, ${EdgesCountTable}
        WHERE ${relation}.id = ${EdgesCountTable}.variable_id"""

      val cardinality = dataType match {
        case BooleanType => 1
        case MultinomialType(x) => x.toLong
      }

      issueQuery(selectVariablesForDumpSQL) { rs =>
        serializer.addVariable(
          rs.getLong(1),            // id
          rs.getBoolean(2),         // is evidence
          rs.getLong(3),            // initial value
          dataType.toString,        // data type
          rs.getLong(4),            // edge count
          cardinality)              // cardinality
        numVariables += 1
        // log.info(rs.getLong(1).toString)
        // log.info(rs.getBoolean(3).toString)
        // log.info(rs.getLong(2).toString)
        // log.info(dataType.toString)
        // log.info(cardinality.toString)
      }

      // TODO: hold out
    }

    log.info("Dumping factors...")

    factorDescs.foreach { factorDesc =>
      val weightValue = factorDesc.weight match { 
        case x : KnownFactorWeight => x.value
        case _ => 0.0
      }
      val isFixed = factorDesc.weight.isInstanceOf[KnownFactorWeight]
      val functionName = factorDesc.func.getClass.getSimpleName

      val selectInputQueryForDumpSQL = s"SELECT * FROM ${factorDesc.name}_query"
      // val variableCols = factorDesc.func.variables.map(v => s"${v.relation}.id")
      val weightVariableCols = factorDesc.weight.variables
      val variables = factorDesc.func.variables

      // variables.foreach { v => 
      //   log.info(i.toString) 
      //   log.info(v) }
      // i += 1

      issueQuery(selectInputQueryForDumpSQL) { rs =>
        val weightCmd = factorDesc.weightPrefix.concat("-").concat(
          weightVariableCols.map(v => rs.getString(v)).mkString(","))
        var weightId : Long = -1

        // log.info(weightCmd)
        if (weightMap.contains(weightCmd)) {
          weightId = weightMap(weightCmd)
        } else {
          weightId = numWeights
          weightMap(weightCmd) = numWeights
          numWeights += 1
          serializer.addWeight(weightId, isFixed, weightValue) 
        }

        //log.info(weightId.toString + isFixed.toString + weightValue.toString)
        serializer.addFactor(numFactors, weightId, functionName, variables.length)

        variables.zipWithIndex.foreach { case(v, pos) =>
          serializer.addEdge(rs.getLong(s"${v.relation}.id"),
            numFactors, pos, !v.isNegated, v.predicate.getOrElse(1))
          numEdges += 1
        }

        numFactors += 1
      }

    }

    serializer.writeMetadata(numWeights, numVariables, numFactors, numEdges,
      weightsPath, variablesPath, factorsPath, edgesPath)

    serializer.close()
  }

  def groundFactorGraph(schema: Map[String, _ <: VariableDataType], factorDescs: Seq[FactorDesc],
    holdoutFraction: Double, holdoutQuery: Option[String], skipLearning: Boolean, weightTable: String) {

    // We write the grounding queries to this SQL file
    val sqlFile = File.createTempFile(s"grounding", ".sql")
    val writer = new PrintWriter(sqlFile)
    log.info(s"""Writing grounding queries to file="${sqlFile}" """)

    // if (skipLearning == true && weightTable.isEmpty()) {
    //   writer.println(copyLastWeightsSQL)
    // }
    // writer.println(createWeightsSQL)
    // writer.println(createFactorsSQL)
    // writer.println(createVariablesSQL)
    writer.println(createVariablesHoldoutSQL)
    // writer.println(createVariablesMapSQL)
    writer.println(createEdgesSQL)
    writer.println(createSequencesSQL)

    // Ground all variables in the schema
    schema.foreach { case(variable, dataType) =>
      val Array(relation, column) = variable.split('.')
      // val cardinalityStr = dataType match {
      //   case BooleanType => "null"
      //   case MultinomialType(x) => x.toString
      // }

      // // FIXME: must guarantee all variable relations have unique ids
      // writer.println(
      //   s"""INSERT INTO ${VariablesTable}(id, data_type, initial_value, is_evidence, cardinality)
      //   SELECT ${relation}.id, '${dataType}', ${variable}::int, (${variable} IS NOT NULL), ${cardinalityStr}
      //   FROM ${relation};""")

      writer.println(s"""
        UPDATE ${relation} SET id = nextval('${IdSequence}');""")

    }

    // // Map variables to sequential IDs
    // writer.println(
    //   s"""INSERT INTO ${VariablesMapTable}(variable_id)
    //   SELECT id FROM ${VariablesTable};""")

    // // Assign the holdout - Random (default) or user-defined query
    // holdoutQuery match {
    //   case None => writer.println(
    //     s"""INSERT INTO ${VariablesHoldoutTable}(variable_id)
    //     SELECT id FROM ${VariablesTable}
    //     WHERE ${randomFunc} < ${holdoutFraction} AND is_evidence = true;""")
    //   case Some(userQuery) => writer.println(userQuery + ";")
    // }
   
    // writer.println(s"""UPDATE ${VariablesTable} SET is_evidence=false
    //   WHERE ${VariablesTable}.id IN (SELECT variable_id FROM ${VariablesHoldoutTable});""")

    // Create table for each inference rule
    factorDescs.zipWithIndex.foreach { case(factorDesc, idx) =>

      // input query
      writer.println(s"""
        DROP TABLE IF EXISTS ${factorDesc.name}_query CASCADE;
        CREATE TABLE ${factorDesc.name}_query AS
          SELECT * FROM (${factorDesc.inputQuery}) AS inputQuery;
        """)
      
      writer.println(s"""
        COMMIT;
        """)

      factorDesc.func.variables.foreach { case variable =>
        val vidColumn = s"${variable.relation}.id"
        writer.println(s"""
          INSERT INTO ${EdgesTable}(variable_id)
          SELECT "${vidColumn}"
          FROM ${factorDesc.name}_query;
        """) 
      }
    }

    // def generateWeightCmd(weightVariables: Seq[String]) : String = {
    //   weightVariables.map(v => s"""(CASE WHEN "${v}" IS NULL THEN '' ELSE "${v}"::text END)""")
    //     .mkString(" || ") match {
    //     case "" => "'-'"
    //     case x  => x
    //   }
    // }

    // def generateWeightCmd(weightPrefix: String, weightVariables: Seq[String]) : String = 
    //   weightVariables.map ( v => s"""(CASE WHEN "${v}" IS NULL THEN '' ELSE "${v}"::text END)""" )
    //     .mkString(" || ") match {
    //     case "" => s"""'${weightPrefix}-'} """
    //     case x => s"""'${weightPrefix}-' || ${x}}"""
    //   }

    // // Ground weights for each inference rule
    // factorDescs.zipWithIndex.foreach { case(factorDesc, idx) =>
    //   val weightValue = factorDesc.weight match { 
    //     case x : KnownFactorWeight => x.value
    //     case _ => 0.0
    //   }

    //   val isFixed = factorDesc.weight.isInstanceOf[KnownFactorWeight]
    //   val weightCmd = generateWeightCmd(factorDesc.weightPrefix, factorDesc.weight.variables)

    //   writer.println(s"""
    //     INSERT INTO ${WeightsTable}(initial_value, is_fixed, factor_group, description)
    //     SELECT DISTINCT ${weightValue} AS wValue, ${isFixed} AS wIsFixed, ${idx} AS iId, ${weightCmd} AS wCmd
    //     FROM ${factorDesc.name}_query GROUP BY wValue, wIsFixed, iId, wCmd;""")

    // }
    
    // Zifei's change: Skip the learning, use last weights
    // If use ID to match: (we do not do this to prevent ID changing)
    // We assume that description does not change and is distinct
    // UPDATE dd_graph_weights SET initial_value = weight FROM dd_inference_result_weights WHERE dd_graph_weights.id = dd_inference_result_weights.id;
    // if (skipLearning == true) {
    //   val fromWeightTable = weightTable.isEmpty() match {
    //     case true => "dd_graph_last_weights"
    //     case false => weightTable
    //   }
    //   log.info(s"""Using weights in TABLE ${fromWeightTable} by matching COLUMN description""")
        
    //   writer.println(s"""
    //     UPDATE dd_graph_weights SET is_fixed = TRUE;
    //     UPDATE dd_graph_weights SET initial_value = weight FROM ${fromWeightTable} 
    //     WHERE dd_graph_weights.description = ${fromWeightTable}.description;
    //     """)
    // }

    // Add index to the weights
    // writer.println(s"""CREATE INDEX ${WeightsTable}_desc_idx ON ${WeightsTable}(description);""")
    // writer.println(s"""ANALYZE ${WeightsTable};""")


    // Ground all factors
    // factorDescs.zipWithIndex.foreach { case(factorDesc, idx) =>

    //   val weightCmd = generateWeightCmd(factorDesc.weightPrefix, factorDesc.weight.variables)
    //   val functionName = factorDesc.func.getClass.getSimpleName

    //   val vid = factorDesc.func.variables.map(v => s""" "${v.relation}.id" """).mkString(",")
    //   val negated = factorDesc.func.variables.map(v => s"${v.isNegated}").mkString(",")

      // writer.println(s"""
      //   INSERT INTO ${FactorsTable}(weight_id, variable_ids, variable_negated, factor_function, factor_group, equal_predicate)
      //   SELECT ${WeightsTable}.id, ARRAY[${vid}], ARRAY[${negated}], '${functionName}', ${idx}, 1
      //   FROM ${factorDesc.name}_query, ${WeightsTable}
      //   WHERE ${idx} = ${WeightsTable}.factor_group AND ${weightCmd} = ${WeightsTable}.description;""")
    // }

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
    // execute(createMappedInferenceResultView)

    log.info("Copying inference result weights...")
    bulkCopyWeights(weightsOutputFile)
    log.info("Copying inference result variables...")
    bulkCopyVariables(variableOutputFile)
    // log.info("Creating indicies on the inference result...")
    // execute(createInferenceResultIndiciesSQL)

    // Each (relation, column) tuple is a variable in the plate model.
     // Find all (relation, column) combinations
    val relationsColumns = variableSchema.keys map (_.split('.')) map {
      case Array(relation, column) => (relation, column)
    }

    // execute(createMappedWeightsViewSQL)

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
