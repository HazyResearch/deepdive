package org.deepdive.ddlog

// DeepDiveLog compiler
// See: https://docs.google.com/document/d/1SBIvvki3mnR28Mf0Pkin9w9mWNam5AA0SpIGj1ZN2c4

// TODO update the following comment to new syntax.
/*
 This file parses an extended form of datalog like sugar.

 It allows schema declarations

 SomeOther(realname, otherattribute)

 And queries

 Q(x,y) :- R(x,y), SomeOther(y, z)

 Using the schema can SQLized as

 SELECT R1.x,R2.y
 FROM   R as R1,SomeOther as R2
 WHERE  R1.y = R2.realname

 We translate by introducing aliases R1, R2 , etc. to deal with
 repeated symbols.

 TODO:
 =================

 Our schema needs to know whether a symbol is this a query table (and
 so should contain an _id) field or is a regular table from the
 user.

 If a head term is not mentioned in the schema, its assumed it is a
 query table that this code must create.

 If one wants to explicilty mention a query table in the schema, they
 do so with a trailing exclamation point as follows

 Q(x,y)!;

Consider

 Q(x) :- R(x,f) weight=f

 ... R is likely *not* a variable table ... we record its translation below.

 In contrast, Q(x) :- R(x),S(x) ... coule be treated as variable tables. Hence, the schema has:

 R(x,f) // regular table
 R(x,f)! // variable table.

 */

/* TODOs:

 Refactor schema object and introduce error checking (unsafe queries,
 unordered attributes, etc.).
*/

import scala.collection.immutable.HashMap
import org.apache.commons.lang3.StringEscapeUtils
import scala.collection.mutable.ListBuffer
import org.deepdive.ddlog.DeepDiveLog.Mode._

object AliasStyle extends Enumeration {
  type AliasStyle = Value
  // Three kinds of column expressions:
  // OriginalOnly => use column name for dd_new_ tables
  // AliasOnly => use alias for unknown weight in inference
  // OriginalAndAlias => use column name and alias for normal extraction rule
  val OriginalOnly, AliasOnly, OriginalAndAlias = Value

}
import AliasStyle._

// This handles the schema statements.
// It can tell you if a predicate is a "query" predicate or a "ground prediate"
// and it resolves Variables their correct and true name in the schema, i.e. R(x,y) then x could be Attribute1 declared.
class CompilationState( statements : DeepDiveLog.Program, config : DeepDiveLog.Config )  {
    // TODO: refactor the schema into a class that constructs and
    // manages these maps. Also it should have appropriate
    // abstractions and error handling for missing values.
    // ** Start refactor.
  var schema : Map[ Tuple2[String,Int], String ] = new HashMap[ Tuple2[String,Int], String ]()

  var ground_relations : Map[ String, Boolean ]  = new HashMap[ String, Boolean ]()

  var function_schema : Map[String, FunctionDeclaration] = new HashMap[ String, FunctionDeclaration]()

  // The dependency graph between statements.
  var dependencies : Map[Statement, Set[Statement]] = new HashMap()

  // The statement whether will compile or union to other statements
  var visible : Set[Statement] = Set()

  var mode : Mode = ORIGINAL

  var useDeltaCount : Boolean = false

  // Mapping head names to the actual statements
  var schemaDeclarationGroupByHead  : Map[String, List[SchemaDeclaration]] = new HashMap[String, List[SchemaDeclaration]]()
  var extractionRuleGroupByHead     : Map[String, List[ExtractionRule]] = new HashMap[String, List[ExtractionRule]]()
  var inferenceRuleGroupByHead      : Map[String, List[InferenceRule]] = new HashMap[String, List[InferenceRule]]()
  var functionCallRuleGroupByInput  : Map[String, List[FunctionCallRule]] = new HashMap[String, List[FunctionCallRule]]()
  var functionCallRuleGroupByOutput : Map[String, List[FunctionCallRule]] = new HashMap[String, List[FunctionCallRule]]()

  def init() = {
    // generate the statements.
    mode = config.mode
    useDeltaCount = mode match {
      case ORIGINAL => false
      case _ => true
    }
    statements.foreach {
      case SchemaDeclaration(Attribute(r, terms, types), isQuery) =>
        terms.foreach {
          case Variable(n,r,i) =>
            schema           += { (r,i) -> n }
            ground_relations += { r -> !isQuery } // record whether a query or a ground term.
        }
      case ExtractionRule(_,_) => ()
      case InferenceRule(_,_,_,_) => ()
      case fdecl : FunctionDeclaration => function_schema += {fdecl.functionName -> fdecl}
      case FunctionCallRule(_,_,_) => ()
    }
    groupByHead(statements)
    analyzeVisible(statements)
    analyzeDependency(statements)
  }

  init()

  def error(message: String) {
    throw new RuntimeException(message)
  }

  // Given a statement, resolve its name for the compiled extractor block.
  def resolveExtractorBlockName(s: Statement): String = {
    s match {
      case s: SchemaDeclaration => s"extraction_rule_${statements indexOf s}"
      case s: FunctionCallRule  => s"extraction_rule_${statements indexOf s}"
      case s: ExtractionRule    => s"extraction_rule_${statements indexOf s}"
      case s: InferenceRule     => s"extraction_rule_${s.q.head.name}"
    }
  }

  // Given an inference rule, resolve its name for the compiled inference block.
  def resolveInferenceBlockName(s: InferenceRule): String = {
    s"factor_${s.q.head.name}_${statements indexOf s}"
  }

  // Given a variable, resolve it.  TODO: This should give a warning,
  // if we encouter a variable that is not in this map, then something
  // odd has happened.
  def resolveName( v : Variable ) : String = {
    v match { case Variable(v,relName,i) =>
      if(schema contains (relName,i)) {
        schema(relName,i)
      } else {
        return v // I do not like this default, as it allows some errors. TOOD: MAKE MORE PRECISE!
      }
    }
  }

  def resolveFunctionName( v : String ) : FunctionDeclaration = {
    function_schema(v)
  }

  // The default is query term.
  def isQueryTerm( relName : String ): Boolean = {
    if( ground_relations contains relName ) !ground_relations(relName) else true
  }

  // Resolve a column name with alias
  def resolveColumn(s: String, qs: QuerySchema, q : ConjunctiveQuery, alias: AliasStyle) : Option[String] = {
    val index = qs.getBodyIndex(s)
    val name  = resolveName(qs.getVar(s))
    val relation = q.bodies(0)(index).name
    alias match {
      case OriginalOnly => Some(s"R${index}.${name}")
      case AliasOnly => Some(s"${relation}.R${index}.${name}")
      case OriginalAndAlias => Some(s"""R${index}.${name} AS "${relation}.R${index}.${name}" """)
    }
  }

  // This is generic code that generates the FROM with positional aliasing R0, R1, etc.
  // and the corresponding WHERE clause (equating all variables)
  def generateSQLBody(z : ConjunctiveQuery) : String = {
    val bodyNames = ( z.bodies(0).zipWithIndex map { case(x,i) => s"${x.name} R${i}"}).mkString(", ")
    // Simple logic for the where clause, first find every first occurence of a
    // and stick it in a map.
    val qs = new QuerySchema(z)

    val whereClause = z.bodies(0).zipWithIndex flatMap {
      case (Atom(relName, terms),body_index) =>
        terms flatMap {
          case Variable(varName, relName, index) =>
            val canonical_body_index = qs.getBodyIndex(varName)

            if (canonical_body_index != body_index) {
              val real_attr_name1 = resolveName( Variable(varName, relName, index) )
              val real_attr_name2 = resolveName( qs.getVar(varName))
              Some(s"R${ body_index }.${ real_attr_name1 } = R${ canonical_body_index }.${ real_attr_name2 } ")
            } else { None }
        }
    }
    val whereClauseStr = whereClause match {
      case Nil => ""
      case _ => s"""WHERE ${whereClause.mkString(" AND ")}"""
    }

    s"""FROM ${ bodyNames }
        ${ whereClauseStr }"""
  }

  // Group statements by head
  def groupByHead(statements: List[Statement]) = {
    // Compile compilation states by head name based on type
    val schemaDeclarationToCompile = new ListBuffer[SchemaDeclaration]()
    val extractionRuleToCompile    = new ListBuffer[ExtractionRule]()
    val inferenceRuleToCompile     = new ListBuffer[InferenceRule]()
    val functionCallRuleToCompile  = new ListBuffer[FunctionCallRule]()

    statements foreach (_ match {
      case s: SchemaDeclaration    => schemaDeclarationToCompile += s
      case s: ExtractionRule       => extractionRuleToCompile += s
      case s: FunctionCallRule     => functionCallRuleToCompile += s
      case s: InferenceRule        => inferenceRuleToCompile += s
      case _                       =>
    })

    schemaDeclarationGroupByHead   = schemaDeclarationToCompile.toList.groupBy(_.a.name)
    extractionRuleGroupByHead      = extractionRuleToCompile.toList.groupBy(_.q.head.name)
    inferenceRuleGroupByHead       = inferenceRuleToCompile.toList.groupBy(_.q.head.name)
    functionCallRuleGroupByInput   = functionCallRuleToCompile.toList.groupBy(_.input)
    functionCallRuleGroupByOutput  = functionCallRuleToCompile.toList.groupBy(_.output)
  }

  // Analyze the block visibility among statements 
  def analyzeVisible(statements: List[Statement]) = {
    extractionRuleGroupByHead   foreach {keyVal => visible += keyVal._2(0)}
    functionCallRuleGroupByInput foreach {keyVal => visible += keyVal._2(0)}
    // inferenceRuleGroupByHead    foreach {keyVal => visible += keyVal._2(0)}
  }

  // Analyze the dependency between statements and construct a graph.
  def analyzeDependency(statements: List[Statement]) = {
    val stmtByHeadName = (extractionRuleGroupByHead.toSeq ++ inferenceRuleGroupByHead.toSeq ++ functionCallRuleGroupByOutput.toSeq).groupBy(_._1).mapValues(_.map(_._2).toList)

    // Look at the body of each statement to construct a dependency graph
    statements foreach {
      case f : FunctionCallRule => dependencies += { f -> ((        Some(f.input) flatMap (stmtByHeadName get _)).toSet.flatten.flatten) }
      case e : ExtractionRule   => dependencies += { e -> ((e.q.bodies.flatten map (_.name) flatMap (stmtByHeadName get _)).toSet.flatten.flatten) }
      case w : InferenceRule    => dependencies += { w -> ((w.q.bodies.flatten map (_.name) flatMap (stmtByHeadName get _)).toSet.flatten.flatten) }
      case _ =>
    }
  }
  // Generates a "dependencies" value for a compiled block of given statement.
  def generateDependenciesOfCompiledBlockFor(statements: List[Statement]): String = {
    var dependentExtractorBlockNames = Set[String]()
    for (statement <- statements) {
      dependentExtractorBlockNames ++= ((dependencies getOrElse (statement, Set())) & visible) map resolveExtractorBlockName
    }
    if (dependentExtractorBlockNames.size == 0) "" else {
      val depStr = dependentExtractorBlockNames map {" \"" + _ + "\" "} mkString(", ")
      s"dependencies: [${depStr}]"
    }
  }
}

// This is responsible for schema elements within a given query, e.g.,
// what is the canonical version of x? (i.e., the first time it is
// mentioned in the body. This is useful to translate to SQL (join
// conditions, select, etc.)
class QuerySchema(q : ConjunctiveQuery) {
    var query_schema = new HashMap[ String, Tuple2[Int,Variable] ]()

  // maps each variable name to a canonical version of itself (first occurence in body in left-to-right order)
  // index is the index of the subgoal/atom this variable is found in the body.
  // variable is the complete Variable type for the found variable.
  def generateCanonicalVar()  = {
    q.bodies(0).zipWithIndex.foreach {
      case (Atom(relName,terms),index) =>  {
        terms.foreach {
          case Variable(v, r, i) =>
            if( ! (query_schema contains v) )
              query_schema += { v -> (index, Variable(v,r,i) ) }
        }
      }
    }
  }
  generateCanonicalVar() // initialize

  // accessors
  def getBodyIndex( varName : String ) : Int = { query_schema(varName)._1 }
  def getVar(varName : String ) : Variable   = { query_schema(varName)._2 }
}


// Compiler that takes parsed program as input and prints blocks of application.conf
object DeepDiveLogCompiler extends DeepDiveLogHandler {
  type CompiledBlock = String
  type CompiledBlocks = List[CompiledBlock]

  // Generate schema for database
  def compileSchemaDeclarations(stmts: List[SchemaDeclaration], ss: CompilationState): CompiledBlocks = {
    var schemas = new ListBuffer[String]()
    for (stmt <- stmts) {
      var columnDecls = stmt.a.terms map {
        case Variable(name, _, i) => s"${name} ${stmt.a.types(i)}"
      }
      if (ss.useDeltaCount && !stmt.isQuery) columnDecls = columnDecls :+ "dd_count int"
      val indentation = " " * stmt.a.name.length
      val blockName = ss.resolveExtractorBlockName(stmt)
      schemas += s"""
        deepdive.extraction.extractors.${blockName} {
          sql: \"\"\" DROP TABLE IF EXISTS ${stmt.a.name} CASCADE;
          CREATE TABLE
          ${stmt.a.name}(${columnDecls.mkString(",\n" + indentation)})
          \"\"\"
          style: "sql_extractor"
        }"""
    }
    schemas.toList
  }

  // Generate extraction rule part for deepdive
  def compileExtractionRules(stmts: List[ExtractionRule], ss: CompilationState): CompiledBlocks = {
    var inputQueries = new ListBuffer[String]()
    for (stmt <- stmts) {
      for (cqBody <- stmt.q.bodies) {
        val tmpCq = ConjunctiveQuery(stmt.q.head, List(cqBody))
        // Generate the body of the query.
        val qs              = new QuerySchema( tmpCq )
        if (ss.inferenceRuleGroupByHead contains stmt.q.head.name) {
          if (stmt.supervision == null) ss.error(s"Cannot find supervision for variable ${stmt.q.head.name}.\n")
          if (stmt.q.bodies.length > 1) ss.error(s"Scoping rule does not allow disjunction.\n")
          val headTerms = tmpCq.head.terms map {
            case Variable(v,r,i) => s"R${i}.${ss.resolveName(qs.getVar(v)) }"
          }
          val index = qs.getBodyIndex(stmt.supervision)
          val name  = ss.resolveName(qs.getVar(stmt.supervision))
          val labelCol = s"R${index}.${name}"
          val headTermsStr = ( "0 as id"  :: headTerms ).mkString(", ")
          val ddCount = if (ss.useDeltaCount) ( tmpCq.bodies(0).zipWithIndex map { case(x,i) => s"R${i}.dd_count"}).mkString(" * ") else ""
          val ddCountStr = if (ddCount.length > 0) s", ${ddCount} AS dd_count" else ""
          inputQueries += s"""SELECT DISTINCT ${ headTermsStr }, ${labelCol} AS label ${ddCountStr}
          ${ ss.generateSQLBody(tmpCq) }
          """
        } else {
          // variable columns
          // dd_new_ tale only need original column name to make sure the schema is the same with original table
          var tmpCqUseOnlyOriginal = ss.mode match {
            case MERGE => true
            case _     => if (tmpCq.head.name.startsWith("dd_new_")) true else false
          }
          val resolveColumnFlag = tmpCqUseOnlyOriginal match {
            case true => OriginalOnly
            case false => OriginalAndAlias
          }
          val variableCols = tmpCq.head.terms flatMap {
            case(Variable(v,rr,i)) => ss.resolveColumn(v, qs, tmpCq, resolveColumnFlag)
          }

          val selectStr = variableCols.mkString(", ")

          var ddCount = if (ss.useDeltaCount) ( tmpCq.bodies(0).zipWithIndex map { case(x,i) => s"R${i}.dd_count"}).mkString(" * ") else ""
          ddCount = ss.mode match {
            case MERGE => s"SUM(${ddCount})"
            case _ => ddCount
          }
          val ddCountStr = if (ddCount.length > 0) {
            if (!tmpCqUseOnlyOriginal) s""", ${ddCount} AS \"dd_count\" """ else s", ${ddCount}"
          } else ""
          val groupBy = ss.mode match {
            case MERGE => s" GROUP BY ${selectStr}"
            case _ => ""
          }
          inputQueries += s"""
            SELECT ${selectStr}${ddCountStr}
            ${ ss.generateSQLBody(tmpCq) }${ groupBy }"""
        }
      }
    }
    val blockName = ss.resolveExtractorBlockName(stmts(0))
    val createTable = ss.mode match {
      case MERGE => true
      case _ => if (ss.schemaDeclarationGroupByHead contains stmts(0).q.head.name) true else false
    }
    val sqlCmdForCleanUp = if (createTable) "TRUNCATE" else "DROP VIEW IF EXISTS"
    val sqlCmdForInsert  = if (createTable) "INSERT INTO" else "CREATE VIEW"
    val useAS            = if (createTable) "" else " AS"
    val cleanUp          = ss.mode match {
      case MERGE => s""";
        DELETE FROM ${stmts(0).q.head.name} WHERE dd_count = 0;"""
      case _     => ""
    }
    val extractor = s"""
      deepdive.extraction.extractors.${blockName} {
        sql: \"\"\" ${sqlCmdForCleanUp} ${stmts(0).q.head.name};
        ${sqlCmdForInsert} ${stmts(0).q.head.name}${useAS} ${inputQueries.mkString(" UNION ")}${cleanUp}
        \"\"\"
        style: "sql_extractor"
          ${ss.generateDependenciesOfCompiledBlockFor(stmts)}
      }
    """
    List(extractor)
  }

  def compileFunctionCallRules(stmts: List[FunctionCallRule], ss: CompilationState): CompiledBlocks = {
    var extractors = new ListBuffer[String]()
    for (stmt <- stmts) {
      val inputQuery = s"""
      SELECT * FROM ${stmt.input}
      """

      val function = ss.resolveFunctionName(stmt.function)
      val udfDetails = (function.implementations collectFirst {
        case impl: RowWiseLineHandler =>
          s"""udf: \"${StringEscapeUtils.escapeJava(impl.command)}\"
          style: \"${impl.format}_extractor\" """
      })

      if (udfDetails.isEmpty)
        ss.error(s"Cannot find compilable implementation for function ${stmt.function} among:\n  "
          + (function.implementations mkString "\n  "))

      val blockName = ss.resolveExtractorBlockName(stmt)
      val extractor = s"""
        deepdive.extraction.extractors.${blockName} {
          input: \"\"\" SELECT * FROM ${stmt.input}
          \"\"\"
          output_relation: \"${stmt.output}\"
          ${udfDetails.get}
          ${ss.generateDependenciesOfCompiledBlockFor(List(stmt))}
        }
      """
      extractors += extractor
    }
    extractors.toList
  }

  // generate inference rule part for deepdive
  def compileInferenceRules(stmts: List[InferenceRule], ss: CompilationState): CompiledBlocks = {
    var blocks = List[String]()
    for (stmt <- stmts) {
      var inputQueries = new ListBuffer[String]()
      var func = ""
      var weight = ""
      for (cqBody <- stmt.q.bodies) {
        // edge query
        val fakeBody        = stmt.q.head +: cqBody
        // val fakeBody        = stmt.q.bodies +: List(stmt.q.head)
        val fakeCQ          = ConjunctiveQuery(stmt.q.head, List(fakeBody)) // we will just use the fakeBody below.

        val index = cqBody.length + 1
        val qs2 = new QuerySchema( fakeCQ )
        val variableIdsStr = Some(s"""R0.id AS "${stmt.q.head.name}.R0.id" """)

        // weight string
        val uwStr = stmt.weights match {
          case KnownFactorWeight(x) => None
          case UnknownFactorWeight(w) => Some(w.flatMap(s => ss.resolveColumn(s, qs2, fakeCQ, OriginalAndAlias)).mkString(", "))
        }

        val selectStr = (List(variableIdsStr, uwStr) flatten).mkString(", ")

        val ddCount = if (ss.useDeltaCount) ( fakeCQ.bodies(0).zipWithIndex map { case(x,i) => s"R${i}.dd_count"}).mkString(" * ") else ""
        val ddCountStr = if (ddCount.length > 0) s""", ${ddCount} AS \"dd_count\" """ else ""

        // factor input query
        inputQueries += s"""
          SELECT ${selectStr} ${ddCountStr}
          ${ ss.generateSQLBody(fakeCQ) }"""
        // factor function
        func = s"""Imply(${stmt.q.head.name}.R0.label)"""
        // weight
        if (weight.length == 0)
          weight = stmt.weights match {
            case KnownFactorWeight(x) => s"${x}"
            case UnknownFactorWeight(w) => {
              s"""?(${w.flatMap(s => ss.resolveColumn(s, qs2, fakeCQ, AliasOnly)).mkString(", ")})"""
            }
          }
      }
      val blockName = ss.resolveInferenceBlockName(stmt)
      blocks ::= s"""
        deepdive.inference.factors.${blockName} {
          input_query: \"\"\"${inputQueries.mkString(" UNION ")}\"\"\"
          function: "${func}"
          weight: "${weight}"
        }
      """
    }
    blocks.reverse
  }


  def compileUserSettings(): CompiledBlocks = {
    // TODO read user's proto-application.conf and augment it
    List("""
  deepdive.db.default {
    driver: "org.postgresql.Driver"
    url: "jdbc:postgresql://"${PGHOST}":"${PGPORT}"/"${DBNAME}
    user: ${PGUSER}
    password: ${PGPASSWORD}
    dbname: ${DBNAME}
    host: ${PGHOST}
    port: ${PGPORT}
  }
  """)
  }

  def compilePipelines(ss: CompilationState): CompiledBlocks = {
    val run = "deepdive.pipeline.run: ${PIPELINE}"
    val setup_database_pipeline = ((ss.schemaDeclarationGroupByHead map (_._2)).flatten map {s => ss.resolveExtractorBlockName(s)}).mkString(", ")
    val initdb = if (setup_database_pipeline.length > 0) s"deepdive.pipeline.pipelines.initdb: [${setup_database_pipeline}]" else ""
    val extraction = (ss.visible map {s => ss.resolveExtractorBlockName(s)}).mkString(", ")
    val extraction_pipeline = if (extraction.length > 0) s"deepdive.pipeline.pipelines.extraction: [${extraction}]" else ""
    val inference = ((ss.inferenceRuleGroupByHead map (_._2)).flatten map {s => ss.resolveInferenceBlockName(s)}).mkString(", ")
    val inference_pipeline = if (inference.length > 0) s"deepdive.pipeline.pipelines.inference: [${inference}]" else ""

    List(run, initdb, extraction_pipeline, inference_pipeline).filter(_ != "")
  }

  // generate variable schema statements
  def compileVariableSchema(statements: DeepDiveLog.Program, ss: CompilationState): CompiledBlocks = {
    var schema = Set[String]()
    // generate the statements.
    statements.foreach {
      case InferenceRule(q, weights, supervision, rule) =>
        val qs = new QuerySchema(q)
        schema += s"${q.head.name}.label: Boolean"
      case _ => ()
    }
    val ddSchema = s"""
      deepdive.schema.variables {
        ${schema.mkString("\n")}
      }
    """
    List(ddSchema)
  }

  // entry point for compilation
  override def run(parsedProgram: DeepDiveLog.Program, config: DeepDiveLog.Config) = {
    // determine the program to compile
    val programToCompile =
      // derive and compile the program based on mode information
      config.mode match {
        case ORIGINAL => parsedProgram
        case INCREMENTAL => DeepDiveLogDeltaDeriver.derive(parsedProgram)
        case MATERIALIZATION => parsedProgram
        case MERGE => DeepDiveLogMergeDeriver.derive(parsedProgram)
      }
    // take an initial pass to analyze the parsed program
    val state = new CompilationState( programToCompile, config )

    val body = new ListBuffer[String]()
    body ++= compileSchemaDeclarations((state.schemaDeclarationGroupByHead map (_._2)).flatten.toList, state)
    state.extractionRuleGroupByHead    foreach {keyVal => body ++= compileExtractionRules(keyVal._2, state)}
    state.functionCallRuleGroupByInput foreach {keyVal => body ++= compileFunctionCallRules(keyVal._2, state)}
    state.inferenceRuleGroupByHead     foreach {keyVal => body ++= compileInferenceRules(keyVal._2, state)}
    
    // compile the program into blocks of application.conf
    val blocks = (
      compileUserSettings
      :::
      compileVariableSchema(programToCompile, state)
      :::
      body.toList
      :::
      compilePipelines(state)
    )

    // emit the generated code
    blocks foreach println

    // if (config.isIncremental) {
    //   // TODO emit extra extractor for moving rows of dd_delta_* to *
    // }
  }
}
