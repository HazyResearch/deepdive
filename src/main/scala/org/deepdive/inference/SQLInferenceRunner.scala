/**
 * Ground the factor graph to file
 *
 * Using the schema and inference rules defined in application.conf, construct factor
 * graph files.
 * Input: variable tables, factor descriptions, holdout configuration, (feature tables)
 * Output: factor graph files: variables, factors, edges, weights, meta that the
 * sampler can read. Refer to deepdive.stanford.edu for more details of the output format.
 * 
 * The workflow is defined in groundFactorGraph. Table and file names are defined in
 * InferenceNamespace
 *
 * We might want to promulgate a new coding standard: no direct SQL queries without a function wrapper, 
 * into which parameters are passed. The goal in doing that would be to enforce type checking by the compiler. 
 * As it stands, if there's a type clash you don't find out until you try to run the query. 
 *
 * DROP TABLE is such a toxic operation. One SHOULD call dataStore.dropAndCreateTable or
 * dataStore.dropAndCreateTableAs in order to drop and create a table. These method will
 * ensure we are only dropping tables inside the DeepDive namespace.
 */
package org.deepdive.inference

import org.deepdive.calibration._
import org.deepdive.datastore.DataLoader
import org.deepdive.settings._

/* Stores the factor graph and inference results. */
trait SQLInferenceRunnerComponent extends InferenceRunnerComponent {
  def inferenceRunner : SQLInferenceRunner
}

trait SQLInferenceRunner extends AbstractInferenceRunner {

  ////////////////////////////////// groundFactorGraph

//  def assignRandom(schema: Map[String, _ <: VariableDataType]) {
//    schema.foreach { case(variable, dataType) =>
//      val Array(relation, column) = variable.split('.')
//        dataStore.assignRandom(relation)
//    }
//  }

  // assign variable id - sequential and unique
  def assignVariablesIds(schema: Map[String, _ <: VariableDataType]) {
    // fast sequential id assign function
    dataStore.createAssignIdFunctionGreenplum()
    dataStore.executeSqlQueries(dataStore.createSequenceFunction(IdSequence))

    var idoffset : Long = 0
    schema.foreach { case(variable, dataType) =>
      val Array(relation, column) = variable.split('.')
      idoffset += dataStore.assignIds(relation, idoffset, IdSequence)
    }
  }
  
  // assign variable holdout
  def assignHoldout(schema: Map[String, _ <: VariableDataType], calibrationSettings: CalibrationSettings) {
    // variable holdout table - if user defined, execute once
    dataStore.dropAndCreateTable(VariablesHoldoutTable, s"variable_id bigint ${dataStore.sqlPrimaryKey}")
    calibrationSettings.holdoutQuery match {
      case Some(query) => {
        log.info("Executing user supplied holdout query")
        dataStore.executeSqlQueries(query)
      }
      case None => {
        log.info("There is no holdout query, will randomly generate holdout set")
         // randomly assign variable holdout
        schema.foreach { case(variable, dataType) =>
          val Array(relation, column) = variable.split('.')
          // This cannot be parsed in def randFunc for now.
          // assign holdout - randomly select from evidence variables of each variable table
          dataStore.executeSqlQueries(s"""
            INSERT INTO ${VariablesHoldoutTable}
            SELECT id FROM ${relation}
            WHERE ${dataStore.randomFunction} < ${calibrationSettings.holdoutFraction}
              AND ${column} IS NOT NULL;""")
        }
      }
    }

    // variable observation table
    dataStore.dropAndCreateTable(VariablesObservationTable, s"variable_id bigint ${dataStore.sqlPrimaryKey}")
    calibrationSettings.observationQuery match {
      case Some(query) => {
        log.info("Executing user supplied observation query")
        dataStore.executeSqlQueries(query)
      }
      case None => {
        log.info("There is no o query")
      }
    }
  }

  // generate cardinality tables for variables in the schema
  // cardinality tables is used to indicate the domains of the variables
  def generateCardinalityTables(schema: Map[String, _ <: VariableDataType]) {
    schema.foreach { case(variable, dataType) =>
      val Array(relation, column) = variable.split('.')
      // note we use five-digit fixed-length representation here
      val cardinalityValues = dataType match {
        case BooleanType => "('00001')"
        case MultinomialType(x) => (0 to x-1).map (n => s"""('${"%05d".format(n)}')""").mkString(", ")
      }
      val cardinalityTableName = InferenceNamespace.getCardinalityTableName(relation, column)
      dataStore.dropAndCreateTable(cardinalityTableName, s"cardinality ${dataStore.sqlDataTypeText}")
      dataStore.executeSqlQueries(s"""
        INSERT INTO ${cardinalityTableName} VALUES ${cardinalityValues};
        """)
    }
  }
  
  // ground variables
  def groundVariables(schema: Map[String, _ <: VariableDataType], du: DataLoader, 
      dbSettings: DbSettings, groundingPath: String) {
        schema.foreach { case(variable, dataType) =>
      val Array(relation, column) = variable.split('.')
      
      val variableDataType = InferenceNamespace.getVariableDataTypeId(dataType)

      // cardinality (domain size) of the variable
      // boolean: 2
      // multinomial: as user defined
      val cardinality = dataType match {
        case BooleanType => 2
        case MultinomialType(x) => x.toInt
      }

      // Create a table to denote variable type - query, evidence, observation
      // variable table join with holdout table 
      // - a variable is an evidence if it has initial value and it is not holdout
      val variableTypeColumn = "__dd_variable_type__"

      // IF:
      //  in observation table, in evidence table => Observation 2
      //  in holdout table => Query 1
      //  not in observation table, not in holdout table, in evidence table => Evidence 1
      //  else => Query 1
      val variableTypeTable = InferenceNamespace.getVariableTypeTableName(relation)
      dataStore.dropAndCreateTableAs(variableTypeTable,
        s"""SELECT t0.id, CASE WHEN t2.variable_id IS NOT NULL AND ${column} IS NOT NULL THEN 2
                           WHEN t1.variable_id IS NOT NULL THEN 0
                           WHEN ${column} IS NOT NULL THEN 1
                           ELSE 0
                      END as ${variableTypeColumn}
        FROM ${relation} t0 LEFT OUTER JOIN ${VariablesHoldoutTable} t1 ON t0.id=t1.variable_id
        LEFT OUTER JOIN ${VariablesObservationTable} t2 ON t0.id=t2.variable_id""")

      // Create an index on the id column of type table to optimize MySQL join, since MySQL uses BNLJ.
      // It's important to tailor join queries for MySQL as they don't have efficient join algorithms.
      // Specifically, we should create indexes on join condition columns (at least in MySQL implementation).
      dataStore.createIndexForJoinOptimization(variableTypeTable, "id")

      // dump variables
      val initvalueCast = dataStore.cast(dataStore.cast(column, "int"), "float")
      // Sen
      // du.unload(s"dd_variables_${relation}", s"${groundingPath}/dd_variables_${relation}",
      val groundingDir = getFileNameFromPath(groundingPath)
      du.unload(InferenceNamespace.getVariableFileName(relation),
        s"${groundingPath}/${InferenceNamespace.getVariableFileName(relation)}",
        dbSettings,
        s"""SELECT t0.id, t1.${variableTypeColumn},
        CASE WHEN t1.${variableTypeColumn} = 0 THEN 0 ELSE ${initvalueCast} END AS initvalue,
        ${variableDataType} AS type, ${cardinality} AS cardinality
        FROM ${relation} t0, ${variableTypeTable} t1
        WHERE t0.id=t1.id
        """, groundingDir)
    }
  }

  // ground factor meta data
  def groundFactorMeta(du: DataLoader, factorDescs: Seq[FactorDesc], dbSettings: DbSettings,
    groundingPath: String) {
    dataStore.dropAndCreateTable(FactorMetaTable, s"name ${dataStore.sqlDataTypeText}, " +
      s"funcid int, sign ${dataStore.sqlDataTypeText}")

    // generate a string containing the signs (whether negated) of variables for each factor
    factorDescs.foreach { factorDesc =>
      val signString = factorDesc.func.variables.map(v => !v.isNegated).mkString(" ")
      val funcid = InferenceNamespace.getFactorFunctionTypeid(factorDesc.func.getClass.getSimpleName)
      dataStore.executeSqlQueries(s"INSERT INTO ${FactorMetaTable} VALUES " +
        s"('${factorDesc.name}', ${funcid}, '${signString}')")
    }

    // dump factor meta data
    val groundingDir = getFileNameFromPath(groundingPath)
    du.unload(InferenceNamespace.getFactorMetaFileName, 
      s"${groundingPath}/${InferenceNamespace.getFactorMetaFileName}", 
      dbSettings, s"SELECT * FROM ${FactorMetaTable}",
      groundingDir)
  }

  // create feature stats for boolean LR function
  def createFeatureStats(factorDesc: FactorDesc) {
    val querytable = InferenceNamespace.getQueryTableName(factorDesc.name)
    val weightDesc = generateWeightDesc(factorDesc.weightPrefix, factorDesc.weight.variables)

    val weightlist = factorDesc.weight.variables.map(v =>
      s""" ${dataStore.quoteColumn(v)} """).mkString(",")

    // Create feature statistics support tables for error analysis,
    // only if it's boolean LR feature (the most common one)
    if (factorDesc.func.variables.length == 1 && factorDesc.func.variableDataType == "Boolean") {
      // This should be a single variable, e.g. "is_true"
      val variableName = getValCols(factorDesc)(0)
      val groupByClause = weightlist match {
        case "" => ""
        case _ => s"GROUP BY ${weightlist}"
      }
      dataStore.executeSqlQueries(s"""
      INSERT INTO ${FeatureStatsSupportTable}
      SELECT ${weightDesc} as description,
             count(CASE WHEN ${variableName}=TRUE THEN 1 ELSE NULL END) AS pos_examples,
             count(CASE WHEN ${variableName}=FALSE THEN 1 ELSE NULL END) AS neg_examples,
             count(CASE WHEN ${variableName} IS NULL THEN 1 ELSE NULL END) AS queries
      FROM ${querytable}
      ${groupByClause};
      """)
      dataStore.executeSqlQueries(dataStore.analyzeTable(FeatureStatsSupportTable))
    }
  }

  /* groundFactorsAndWeights methods */

  def copyLastWeights = {
    dataStore.executeSqlQueries(s"""
      DROP TABLE IF EXISTS ${lastWeightsTable} ${dataStore.sqlCascade};
      CREATE TABLE ${lastWeightsTable} AS
        SELECT X.*, Y.weight
        FROM ${WeightsTable} AS X INNER JOIN ${WeightResultTable} AS Y ON X.id = Y.id;
      """)
  }

  def createWeightsTable =
    dataStore.dropAndCreateTable(WeightsTable, s"""
      id bigint, isfixed int, initvalue real, cardinality ${dataStore.sqlDataTypeText},
      description ${dataStore.sqlDataTypeText}""")

  def createSequenceFunction(name:String) =
    dataStore.executeSqlQueries(dataStore.createSequenceFunction(name))

  def createFactorQueryTableWithId(factorDesc:FactorDesc, startId:Long, sequenceName:String): Long = {
    val querytable = InferenceNamespace.getQueryTableName(factorDesc.name)

    // table of input query
    dataStore.dropAndCreateTableAs(querytable, factorDesc.inputQuery)
    dataStore.executeSqlQueries(s"""ALTER TABLE ${querytable} ADD COLUMN id bigint;""")

    // handle factor id
    dataStore.assignIds(querytable.toLowerCase(), startId, sequenceName)
  }

  def generateWeightDesc(weightPrefix: String, weightVariables: Seq[String]) : String = {
    dataStore.concat(weightVariables.map ( v =>
      s"""(CASE WHEN ${dataStore.quoteColumn(v)} IS NULL THEN '' ELSE ${dataStore.cast(dataStore.quoteColumn(v),
        dataStore.sqlDataTypeText)} END)""" ),
      "-") // Delimiter '-' for concat
    match {
      case "" => s"'${weightPrefix}-' "
      // concatenate the "prefix-" with the weight values
      case x => dataStore.concat(Seq(s"'${weightPrefix}-' ", x), "")
    }
  }

  def getIdCols(factorDesc:FactorDesc) = factorDesc.func.variables.map(v =>
     s""" ${dataStore.quoteColumn(s"${v.relation}.id")} """)

  def getValCols(factorDesc:FactorDesc) = factorDesc.func.variables.map(v =>
     s""" ${dataStore.quoteColumn(v.toString)} """)

  def createBooleanFactorWeightTableWithId(factorDesc:FactorDesc, cweightid:Long, weightidSequence:String,
                                           du:DataLoader, groundingPath:String, dbSettings: DbSettings):Long = {
    val factorQueryTable = InferenceNamespace.getQueryTableName(factorDesc.name)
    val factorWeightTable = InferenceNamespace.getWeightTableName(factorDesc.name)

    val idcols = getIdCols(factorDesc)

    //factorDesc.func.variables.zipWithIndex.map { case (v,i) => s"id$i" }
    //val valcols = factorDesc.func.variables.zipWithIndex.map { case (v,i) => s"v$i" }
    //val typ = factorDesc.func.variableDataType match {
    //    case "Boolean" => "boolean"
    //    case "Discrete" => "int"
    //}
    //val coldefstr = factorDesc.func.variables.zipWithIndex.map { case (v,i) => s"id$i bigint, v$i $typ" }.mkString(", ")

    // weight variable list
    val hasWeightVariables = !factorDesc.weight.variables.isEmpty
    val isFixed = factorDesc.weight.isInstanceOf[KnownFactorWeight]
    val initvalue = factorDesc.weight match {
      case x : KnownFactorWeight => x.value
      case _ => 0.0
    }
    val weightlist = factorDesc.weight.variables.map(v =>
      s""" ${dataStore.quoteColumn(v)} """).mkString(",")

    // branch if weight variables present
    val notFixedAndHasWeightVariables = !isFixed && hasWeightVariables
    notFixedAndHasWeightVariables match {
      // create a table that only contains one row (one weight)
      case false => dataStore.dropAndCreateTableAs(factorWeightTable,
        s"""SELECT ${dataStore.cast(isFixed, "int")} AS isfixed, ${dataStore.cast(initvalue, "float")} AS initvalue,
                ${dataStore.cast(0, "bigint")} AS id;""")
      // create one weight for each different element in weightlist.
      case true =>
        dataStore.dropAndCreateTableAs(factorWeightTable,
        s"""SELECT ${weightlist}, ${dataStore.cast(isFixed, "int")} AS isfixed,
              ${dataStore.cast(initvalue, "float")} AS initvalue, ${dataStore.cast(0, "bigint")} AS id
              FROM ${factorQueryTable}
              GROUP BY ${weightlist}""")
    }

    // handle weight id
    val count = dataStore.assignIds(factorWeightTable.toLowerCase(), cweightid, weightidSequence)

    val weightDesc = generateWeightDesc(factorDesc.weightPrefix, factorDesc.weight.variables)

    dataStore.executeSqlQueries(s"""INSERT INTO ${WeightsTable}(id, isfixed, initvalue, description)
            SELECT id, isfixed, initvalue, ${weightDesc} FROM ${factorWeightTable};""")

    // check null weight (only if there are weight variables)
    if (notFixedAndHasWeightVariables) {
      val weightChecklist = factorDesc.weight.variables.map(v => s""" ${dataStore.quoteColumn(v)} IS NULL """).mkString("AND")
      dataStore.executeSqlQueryWithCallback(s"SELECT COUNT(*) FROM ${factorQueryTable} WHERE ${weightChecklist}") { rs =>
        if (rs.getLong(1) > 0) {
          throw new RuntimeException("Weight variable has null values")
        }
      }
    }

    dataStore.executeSqlQueries(dataStore.analyzeTable(factorQueryTable))
    dataStore.executeSqlQueries(dataStore.analyzeTable(factorWeightTable))

    // dump factors
    val weightjoinlist = factorDesc.weight.variables.map(
      v => s""" t0.${dataStore.quoteColumn(v)} = t1.${dataStore.quoteColumn(v)} """).mkString("AND")
    // do not have join conditions if there are no weight variables, and t1 will only have 1 row
    val weightJoinCondition = hasWeightVariables match {
      case true => "WHERE " + factorDesc.weight.variables.map(
        v => s""" t0.${dataStore.quoteColumn(v)} = t1.${dataStore.quoteColumn(v)} """).mkString("AND")
      case false => ""
    }

    //val idcols = factorDesc.func.variables.map(v =>
    //  s""" ${dataStore.quoteColumn(s"${v.relation}.id")} """).mkString(", ")
    val outfile = InferenceNamespace.getFactorFileName(factorDesc.name)
    val groundingDir = getFileNameFromPath(groundingPath)
    du.unload(s"${outfile}", s"${groundingPath}/${outfile}", dbSettings, s"""
      SELECT t0.id AS factor_id, t1.id AS weight_id, ${idcols.mkString(", ")}
      FROM ${factorQueryTable} t0, ${factorWeightTable} t1 ${weightJoinCondition};""", groundingDir)

    return count
  }

//  def createMultinomialFactorWeightTableWithId(factorDesc:FactorDesc, cweightid:Long, weightidSequence:String,
//                                                du:DataLoader, groundingPath:String, dbSettings: DbSettings):Long ={
//    val factorQueryTable = InferenceNamespace.getQueryTableName(factorDesc.name)
//    val factorWeightTable = InferenceNamespace.getWeightTableName(factorDesc.name)
//
//    val hasWeightVariables = !factorDesc.weight.variables.isEmpty
//    val isFixed = factorDesc.weight.isInstanceOf[KnownFactorWeight]
//    val initvalue = factorDesc.weight match {
//      case x : KnownFactorWeight => x.value
//      case _ => 0.0
//    }
//    val weightlist = factorDesc.weight.variables.map(v =>
//      s""" ${dataStore.quoteColumn(v)} """).mkString(",")
//
//    // TODO needs better code reuse
//    // handle multinomial
//    // generate cardinality table for each variable
//    factorDesc.func.variables.zipWithIndex.foreach { case(v,idx) =>
//      val cardinalityTableName = InferenceNamespace.getCardinalityInFactorTableName(
//        factorDesc.weightPrefix, idx)
//      dataStore.dropAndCreateTableAs(cardinalityTableName, s"""SELECT * FROM
//            ${InferenceNamespace.getCardinalityTableName(v.headRelation, v.field)};""")
//    }


  def createMultinomialFactorWeightTableWithId(factorDesc:FactorDesc, cweightid:Long, weightidSequence:String,
                                                du:DataLoader, groundingPath:String, dbSettings: DbSettings):Long ={
    val factorQueryTable = InferenceNamespace.getQueryTableName(factorDesc.name)
    val factorWeightTable = InferenceNamespace.getWeightTableName(factorDesc.name)

    val hasWeightVariables = !factorDesc.weight.variables.isEmpty
    val isFixed = factorDesc.weight.isInstanceOf[KnownFactorWeight]
    val initvalue = factorDesc.weight match {
      case x : KnownFactorWeight => x.value
      case _ => 0.0
    }
    val weightlist = factorDesc.weight.variables.map(v =>
      s""" ${dataStore.quoteColumn(v)} """).mkString(",")

    // TODO needs better code reuse
    // handle multinomial
    // generate cardinality table for each variable
    factorDesc.func.variables.zipWithIndex.foreach { case(v,idx) =>
      val cardinalityTableName = InferenceNamespace.getCardinalityInFactorTableName(
        factorDesc.weightPrefix, idx)
      dataStore.dropAndCreateTableAs(cardinalityTableName, s"""SELECT * FROM
            ${InferenceNamespace.getCardinalityTableName(v.headRelation, v.field)};""")
    }

    // cardinality values used in weight
    val cardinalityValues = factorDesc.func.variables.zipWithIndex.map { case(v,idx) =>
      s"""_c${idx}.cardinality"""
    }
    val cardinalityTables = factorDesc.func.variables.zipWithIndex.map { case(v,idx) =>
      s"${InferenceNamespace.getCardinalityInFactorTableName(factorDesc.weightPrefix, idx)} AS _c${idx}"
    }
    val cardinalityCmd = s"""${dataStore.concat(cardinalityValues,",")}"""

    // handle weights table
    // weight is fixed, or doesn't have weight variables
    if (isFixed || !hasWeightVariables){
      dataStore.dropAndCreateTableAs(factorWeightTable, s"""
            SELECT ${dataStore.cast(isFixed, "int")} AS isfixed, ${initvalue} AS initvalue,
            ${cardinalityCmd} AS cardinality, ${cweightid} AS id
            FROM ${cardinalityTables.mkString(", ")}
            ORDER BY cardinality""")

      // handle weight id
      val count = dataStore.assignIds(factorWeightTable.toLowerCase(), cweightid, weightidSequence)

      val weightDesc = generateWeightDesc(factorDesc.weightPrefix, factorDesc.weight.variables)
      dataStore.executeSqlQueries(s"""INSERT INTO ${WeightsTable}(id, isfixed, initvalue, cardinality, description)
            SELECT id, isfixed, initvalue, cardinality, ${weightDesc} FROM ${factorWeightTable};""")

      val idcols = getIdCols(factorDesc)

      //val idcols = factorDesc.func.variables.map(v =>
      //  s""" ${dataStore.quoteColumn(s"${v.relation}.id")} """).mkString(", ")
      val outfile = InferenceNamespace.getFactorFileName(factorDesc.name)
      val groundingDir = getFileNameFromPath(groundingPath)
      du.unload(s"${outfile}", s"${groundingPath}/${outfile}", dbSettings,
        s"SELECT id AS factor_id, ${cweightid} AS weight_id, ${idcols.mkString(", ")} FROM ${factorQueryTable}",
        groundingDir)

      // increment weight id
      return count

    } else { // not fixed and has weight variables
      // temporary weight table for weights without a cross product with cardinality
      val weighttableForThisFactorTemp = s"dd_weight_${factorDesc.name}_temp"

      dataStore.dropAndCreateTableAs(weighttableForThisFactorTemp,
        s"""SELECT ${weightlist}, ${dataStore.cast(isFixed, "int")} AS isfixed,
            ${dataStore.cast(initvalue, "float")} AS initvalue
            FROM ${factorQueryTable}
            GROUP BY ${weightlist}""")

      // We need to create two tables -- one for a non-order'ed version
      // another for an ordered version. The reason that we cannot
      // do this with only one table is not fundemental -- it is just
      // a specific property of Greenplum to make it right
      dataStore.dropAndCreateTableAs(s"${factorWeightTable}_unsorted", s"""
        SELECT ${weighttableForThisFactorTemp}.*, ${cardinalityCmd} AS cardinality,
          ${dataStore.cast(0, "bigint")} AS id
        FROM ${weighttableForThisFactorTemp}, ${cardinalityTables.mkString(", ")} LIMIT 0;""")

      dataStore.executeSqlQueries(s"""
        INSERT INTO ${factorWeightTable}_unsorted
        SELECT ${weighttableForThisFactorTemp}.*, ${cardinalityCmd} as cardinality, 0 AS id
        FROM ${weighttableForThisFactorTemp}, ${cardinalityTables.mkString(", ")}
        ORDER BY ${weightlist}, cardinality;""")

      dataStore.dropAndCreateTableAs(factorWeightTable, s"""
        SELECT ${weighttableForThisFactorTemp}.*, ${cardinalityCmd} AS cardinality,
          ${dataStore.cast(0, "bigint")} AS id
        FROM ${weighttableForThisFactorTemp}, ${cardinalityTables.mkString(", ")} LIMIT 0""")

      dataStore.executeSqlQueries(s"""
        INSERT INTO ${factorWeightTable}
        SELECT * FROM ${factorWeightTable}_unsorted
        ORDER BY ${weightlist}, cardinality;""")

      // handle weight id
      val count = dataStore.assignIds(factorWeightTable.toLowerCase(), cweightid, weightidSequence)

      val weightDesc = generateWeightDesc(factorDesc.weightPrefix, factorDesc.weight.variables)
      dataStore.executeSqlQueries(s"""INSERT INTO ${WeightsTable}(id, isfixed, initvalue, cardinality, description)
            SELECT id, isfixed, initvalue, cardinality, ${weightDesc} FROM ${factorWeightTable};""")

      // use weight id corresponding to cardinality 0 (like C array...)
      val cardinalityKey = factorDesc.func.variables.map(v => "00000").mkString(",")

      // dump factors
      // TODO we don't have enough code reuse here.
      val weightjoinlist = factorDesc.weight.variables.map(v =>
        s""" t0.${dataStore.quoteColumn(v)} = t1.${dataStore.quoteColumn(v)} """).mkString("AND")
      dataStore.executeSqlQueries(dataStore.analyzeTable(factorQueryTable))
      dataStore.executeSqlQueries(dataStore.analyzeTable(factorWeightTable))

      val idcols = getIdCols(factorDesc)

      //val idcols = factorDesc.func.variables.map(v =>
      //  s""" ${dataStore.quoteColumn(s"${v.relation}.id")} """).mkString(", ")
      val outfile = InferenceNamespace.getFactorFileName(factorDesc.name)
      val groundingDir = getFileNameFromPath(groundingPath)
      du.unload(s"${outfile}", s"${groundingPath}/${outfile}", dbSettings,
        s"""SELECT t0.id AS factor_id, t1.id AS weight_id, ${idcols.mkString(", ")}
             FROM ${factorQueryTable} t0, ${factorWeightTable} t1
             WHERE ${weightjoinlist} AND t1.cardinality = '${cardinalityKey}';""",
        groundingDir)

      return count
    }
  }

  def dumpWeights(du:DataLoader, groundingPath:String, dbSettings: DbSettings) = {
    val outfile = InferenceNamespace.getWeightFileName
    val groundingDir = getFileNameFromPath(groundingPath)
    du.unload(outfile, groundingPath + "/" + outfile, dbSettings,
      s"SELECT id, isfixed, COALESCE(initvalue, 0) FROM ${WeightsTable}",
      groundingDir)
  }

  // handle reusing last weights
  def reuseWeights(weightTable: String) {
    // skip learning: choose a table to copy weights from
    val fromWeightTable = weightTable.isEmpty() match {
      case true => lastWeightsTable
      case false => weightTable
    }
    log.info(s"""Using weights in TABLE ${fromWeightTable} by matching description""")

    // Already set -l 0 for sampler
    dataStore.executeSqlQueries(s"""
      UPDATE ${WeightsTable} SET initvalue = weight 
      FROM ${fromWeightTable} 
      WHERE ${WeightsTable}.description = ${fromWeightTable}.description;
      """)
  }

  /* Calibration methods */

  def createBucketedCalibrationView(name: String, inferenceViewName: String, buckets: List[Bucket]) = {
    val bucketCaseStatement = buckets.zipWithIndex.map { case(bucket, index) =>
      s"WHEN expectation >= ${bucket.from} AND expectation <= ${bucket.to} THEN ${index}"
    }.mkString("\n")
    val sql = s"""
      DROP VIEW IF EXISTS ${name};
      CREATE VIEW ${name} AS
      SELECT ${inferenceViewName}.*, CASE ${bucketCaseStatement} END bucket
      FROM ${inferenceViewName} ORDER BY bucket ASC;"""
    dataStore.executeSqlQueries(sql)
  }

  def selectCalibrationData(name: String):List[Map[String, Any]] =
    dataStore.selectAsMap(s"""
      SELECT bucket as "bucket", num_variables AS "num_variables",
        num_correct AS "num_correct", num_incorrect AS "num_incorrect"
      FROM ${name};""")

  def createCalibrationViewBoolean(name: String, bucketedView: String, columnName: String) =
    // Note: MySQL doesn't allow subqueries in from clause of view; must override
    // in MySQL implementation
    dataStore.executeSqlQueries(s"""
      DROP VIEW IF EXISTS ${name};
      CREATE VIEW ${name} AS
      SELECT b1.bucket, b1.num_variables, b2.num_correct, b3.num_incorrect FROM
      (SELECT bucket, COUNT(*) AS num_variables from ${bucketedView} GROUP BY bucket) b1
      LEFT JOIN (SELECT bucket, COUNT(*) AS num_correct from ${bucketedView}
        WHERE ${columnName}=true GROUP BY bucket) b2 ON b1.bucket = b2.bucket
      LEFT JOIN (SELECT bucket, COUNT(*) AS num_incorrect from ${bucketedView}
        WHERE ${columnName}=false GROUP BY bucket) b3 ON b1.bucket = b3.bucket
      ORDER BY b1.bucket ASC;""")

  def createCalibrationViewMultinomial(name: String, bucketedView: String, columnName: String) =
    // Note: MySQL doesn't allow subqueries in from clause of view; must override
    // in MySQL implementation
    dataStore.executeSqlQueries(s"""
      DROP VIEW IF EXISTS ${name};
      CREATE VIEW ${name} AS
      SELECT b1.bucket, b1.num_variables, b2.num_correct, b3.num_incorrect FROM
      (SELECT bucket, COUNT(*) AS num_variables from ${bucketedView} GROUP BY bucket) b1
      LEFT JOIN (SELECT bucket, COUNT(*) AS num_correct from ${bucketedView}
        WHERE ${columnName} = category GROUP BY bucket) b2 ON b1.bucket = b2.bucket
      LEFT JOIN (SELECT bucket, COUNT(*) AS num_incorrect from ${bucketedView}
        WHERE ${columnName} != category GROUP BY bucket) b3 ON b1.bucket = b3.bucket
      ORDER BY b1.bucket ASC;""")


  /* writebackInferenceResult methods */

  def createInferenceResult =
    dataStore.executeSqlQueries(s"""
      DROP TABLE IF EXISTS ${VariableResultTable} ${dataStore.sqlCascade};
      CREATE TABLE ${VariableResultTable}(
        id bigint,
        category bigint,
        expectation ${dataStore.sqlDataTypeDouble}) ${dataStore.sqlStoreAsText};""")

  def createInferenceResultWeights =
    dataStore.executeSqlQueries(s"""
      DROP TABLE IF EXISTS ${WeightResultTable} ${dataStore.sqlCascade};
      CREATE TABLE ${WeightResultTable}(
        id bigint ${dataStore.sqlPrimaryKey},
        weight ${dataStore.sqlDataTypeDouble}) ${dataStore.sqlStoreAsText};""")

  def createInferenceView(relationName: String, columnName: String) =
    dataStore.executeSqlQueries(s"""
      DROP VIEW IF EXISTS ${relationName}_${columnName}_inference;
      CREATE VIEW ${relationName}_${columnName}_inference AS
      (SELECT ${relationName}.*, mir.category, mir.expectation FROM
      ${relationName}, ${VariableResultTable} mir
      WHERE ${relationName}.id = mir.id
      ORDER BY mir.expectation DESC);""")

  /**
   * Create a view that shows weights of features as well as their supports
   */
  def createMappedFeatureStatsView =
    dataStore.executeSqlQueries(s"""
      DROP VIEW IF EXISTS ${FeatureStatsView};
      CREATE VIEW ${FeatureStatsView} AS
      SELECT w.*, f.pos_examples, f.neg_examples, f.queries
      FROM ${LearnedWeightsTable} w LEFT OUTER JOIN ${FeatureStatsSupportTable} f
      ON w.description = f.description
      ORDER BY abs(weight) DESC;""")

  /**
   * Create a table of how LR features are supported by supervision examples
   */
  def createFeatureStatsSupportTable =
    dataStore.executeSqlQueries(s"""
      DROP TABLE IF EXISTS ${FeatureStatsSupportTable} ${dataStore.sqlCascade};
      CREATE TABLE ${FeatureStatsSupportTable}(
        description ${dataStore.sqlDataTypeText},
        pos_examples bigint,
        neg_examples bigint,
        queries bigint);""")

  def createMappedWeightsView =
    dataStore.executeSqlQueries(s"""
      DROP VIEW IF EXISTS ${LearnedWeightsTable};
      CREATE VIEW ${LearnedWeightsTable} AS
      SELECT ${WeightsTable}.*, ${WeightResultTable}.weight FROM
      ${WeightsTable} JOIN ${WeightResultTable} ON ${WeightsTable}.id = ${WeightResultTable}.id
      ORDER BY abs(weight) DESC;

      DROP VIEW IF EXISTS ${VariableResultTable}_mapped_weights;
      CREATE VIEW ${VariableResultTable}_mapped_weights AS
      SELECT * FROM ${LearnedWeightsTable}
      ORDER BY abs(weight) DESC;""")
}
