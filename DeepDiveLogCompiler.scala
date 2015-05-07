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

// This handles the schema statements.
// It can tell you if a predicate is a "query" predicate or a "ground prediate"
// and it resolves Variables their correct and true name in the schema, i.e. R(x,y) then x could be Attribute1 declared.
class CompilationState( statements : DeepDiveLog.Program )  {
    // TODO: refactor the schema into a class that constructs and
    // manages these maps. Also it should have appropriate
    // abstractions and error handling for missing values.
    // ** Start refactor.
  var schema : Map[ Tuple2[String,Int], String ] = new HashMap[ Tuple2[String,Int], String ]()

  var ground_relations : Map[ String, Boolean ]  = new HashMap[ String, Boolean ]()

  var function_schema : Map[String, FunctionDeclaration] = new HashMap[ String, FunctionDeclaration]()

  // The dependency graph between statements.
  var dependencies : Map[Statement, Set[Statement]] = new HashMap()

  var visable : Set[Statement] = Set()

  def init() = {
    // generate the statements.
    statements.foreach {
      case SchemaDeclaration(Attribute(r, terms, types), isQuery) =>
        terms.foreach {
          case Variable(n,r,i) =>
            schema           += { (r,i) -> n }
            ground_relations += { r -> !isQuery } // record whether a query or a ground term.
        }
      case ExtractionRule(_) => ()
      case InferenceRule(_,_,_) => ()
      case fdecl : FunctionDeclaration => function_schema += {fdecl.functionName -> fdecl}
      case FunctionCallRule(_,_,_) => ()
    }

    analyzeVisable(statements)
    analyzeDependency(statements)
  }

  init()

  def error(message: String) {
    throw new RuntimeException(message)
  }

  // Given a statement, resolve its name for the compiled extractor block.
  def resolveExtractorBlockName(s: Statement): String = {
    if (visable contains s) {
      s match {
        case s: FunctionCallRule => s"extraction_rule_${statements indexOf s}"
        case s: ExtractionRule   => s"extraction_rule_${statements indexOf s}"
        case s: InferenceRule    => s"extraction_rule_${s.q.head.name}"
      }
    } else ""
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

  // resolve a column name with alias
  def resolveColumn(s: String, qs: QuerySchema, q : ConjunctiveQuery, alias: Boolean) : Option[String] = {
    val index = qs.getBodyIndex(s)
    val name  = resolveName(qs.getVar(s))
    val relation = q.body(index).name
    if (alias)
      Some(s"""R${index}.${name} AS "${relation}.R${index}.${name}" """)
    else
      Some(s"${relation}.R${index}.${name}")
  }

  // This is generic code that generates the FROM with positional aliasing R0, R1, etc.
  // and the corresponding WHERE clause (equating all variables)
  def generateSQLBody(z : ConjunctiveQuery) : String = {
    val bodyNames = ( z.body.zipWithIndex map { case(x,i) => s"${x.name} R${i}"}).mkString(", ")
    // Simple logic for the where clause, first find every first occurence of a
    // and stick it in a map.
    val qs = new QuerySchema(z)

    val whereClause = z.body.zipWithIndex flatMap {
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

  // Analyze the block visibility among statements 
  def analyzeVisable(statements: List[Statement]) = {
    val extractionRules   = new ListBuffer[ExtractionRule]()
    val functionCallRules = new ListBuffer[FunctionCallRule]()
    val inferenceRules    = new ListBuffer[InferenceRule]()
    
    statements foreach (_ match {
      case s: ExtractionRule   => extractionRules += s
      case s: FunctionCallRule => functionCallRules += s
      case s: InferenceRule    => inferenceRules += s
      case _                   => 
    })

    val extractionRulesGroup   = extractionRules.groupBy(_.q.head.name)
    val functionCallRulesGroup = functionCallRules.groupBy(_.input)
    val inferenceRulesGroup    = inferenceRules.groupBy(_.q.head.name)

    extractionRulesGroup   foreach {keyVal => visable += keyVal._2(0)}
    functionCallRulesGroup foreach {keyVal => visable += keyVal._2(0)}
    inferenceRulesGroup    foreach {keyVal => visable += keyVal._2(0)}
  }

  // Analyze the dependency between statements and construct a graph.
  def analyzeDependency(statements: List[Statement]) = {
    // first map head names to the actual statement
    val extractionRuleByHeadName   = new ListBuffer[ExtractionRule]()
    val inferenceRuleByHeadName    = new ListBuffer[InferenceRule]()
    val functionCallRuleByHeadName = new ListBuffer[FunctionCallRule]()
    
    statements foreach {
      case s: ExtractionRule   => extractionRuleByHeadName += s
      case s: FunctionCallRule => functionCallRuleByHeadName += s
      case s: InferenceRule    => inferenceRuleByHeadName += s
      case _                   => 
    }

    val eByHeadNameGroup = extractionRuleByHeadName.toList.groupBy(_.q.head.name)
    val iByHeadNameGroup = inferenceRuleByHeadName.toList.groupBy(_.q.head.name)
    val fByHeadNameGroup = functionCallRuleByHeadName.toList.groupBy(_.output)

    var stmtByHeadName = (eByHeadNameGroup.toSeq ++ iByHeadNameGroup.toSeq ++ fByHeadNameGroup.toSeq).groupBy(_._1).mapValues(_.map(_._2).toList)
    // var stmtByHeadName = new HashMap[String, Statement]()
    // statements foreach {
    //   case e : ExtractionRule => stmtByHeadName += { e.q.head.name -> e }
    //   case f : FunctionCallRule   => stmtByHeadName += { f.output      -> f }
    //   case w : InferenceRule  => stmtByHeadName += { w.q.head.name -> w }
    //   case _ =>
    // }
    // then, look at the body of each statement to construct a dependency graph
    statements foreach {
      case f : FunctionCallRule => dependencies += { f -> ((        Some(f.input) flatMap (stmtByHeadName get _)).toSet.flatten.flatten) }
      case e : ExtractionRule   => dependencies += { e -> ((e.q.body map (_.name) flatMap (stmtByHeadName get _)).toSet.flatten.flatten) }
      case w : InferenceRule    => dependencies += { w -> ((w.q.body map (_.name) flatMap (stmtByHeadName get _)).toSet.flatten.flatten) }
      case _ =>
    }
  }
  // Generates a "dependencies" value for a compiled block of given statement.
  def generateDependenciesOfCompiledBlockFor(statements: List[Statement]): String = {
    var dependentExtractorBlockNames = Set[String]()
    for (statement <- statements) {
      dependentExtractorBlockNames ++= ((dependencies getOrElse (statement, Set())) & visable) map resolveExtractorBlockName
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
    q.body.zipWithIndex.foreach {
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

  // Dispatch to the corresponding compile function
  // def compile(stmts: List[Statement], ss: CompilationState, isIncremental: Boolean): CompiledBlocks = stmts(0) match {
  //   case s: ExtractionRule   => compileE(stmts, ss, isIncremental)
  //   case s: FunctionCallRule => compileF(stmts, ss, isIncremental)
  //   case s: InferenceRule    => compileI(stmts, ss, isIncremental)
  //   case _                   => List()  // defaults to compiling into empty block
  // }

  // Generate extraction rule part for deepdive
  def compileE(stmts: List[ExtractionRule], ss: CompilationState, isIncremental: Boolean): CompiledBlocks = {
    var inputQueries = new ListBuffer[String]()
    for (stmt <- stmts) {
      // Generate the body of the query.
      val qs              = new QuerySchema( stmt.q )
      // variable columns
      val variableCols = stmt.q.head.terms flatMap {
        case(Variable(v,rr,i)) => ss.resolveColumn(v, qs, stmt.q, true)
      }

      val variableColsStr = if (variableCols.length > 0) Some(variableCols.mkString(", ")) else None

      val selectStr = (List(variableColsStr) flatMap (u => u)).mkString(", ")

      val ddCount = if (isIncremental) ( stmt.q.body.zipWithIndex map { case(x,i) => s"R${i}.dd_count"}).mkString(" * ") else ""
      val ddCountStr = if (ddCount.length > 0) s""", ${ddCount} AS \"dd_count\" """ else ""

      inputQueries += s"""
        SELECT ${selectStr}${ddCountStr}
        ${ ss.generateSQLBody(stmt.q) }"""

    }
    val blockName = ss.resolveExtractorBlockName(stmts(0))
    val extractor = s"""
      deepdive.extraction.extractors.${blockName} {
        sql: \"\"\" DROP VIEW IF EXISTS ${stmts(0).q.head.name};
        CREATE VIEW ${stmts(0).q.head.name} AS ${inputQueries.mkString(" UNION ")}
        \"\"\"
        style: "sql_extractor"
          ${ss.generateDependenciesOfCompiledBlockFor(stmts)}
      }
    """
    List(extractor)
  }

  def compileF(stmts: List[FunctionCallRule], ss: CompilationState, isIncremental: Boolean): CompiledBlocks = {
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
  def compileI(stmts: List[InferenceRule], ss: CompilationState, isIncremental: Boolean): CompiledBlocks = {
    var blocks = List[String]()
    val qs = new QuerySchema( stmts(0).q )
    // node query
    // generate the node portion (V) of the factor graph
    def compileNodeRule(zs: List[InferenceRule], qs: QuerySchema, ss: CompilationState) : CompiledBlocks = {
      var inputQueries = new ListBuffer[String]()
      for (z <- zs) {
        val headTerms = z.q.head.terms map {
          case Variable(v,r,i) => s"R${i}.${ss.resolveName(qs.getVar(v)) }"
        }
        val index = qs.getBodyIndex(z.supervision)
        val name  = ss.resolveName(qs.getVar(z.supervision))
        val labelCol = s"R${index}.${name}"
        val headTermsStr = ( "0 as id"  :: headTerms ).mkString(", ")
        val ddCount = if (isIncremental) ( z.q.body.zipWithIndex map { case(x,i) => s"R${i}.dd_count"}).mkString(" * ") else ""
        val ddCountStr = if (ddCount.length > 0) s", ${ddCount} AS dd_count" else ""

        inputQueries += s"""SELECT DISTINCT ${ headTermsStr }, ${labelCol} AS label ${ddCountStr}
        ${ ss.generateSQLBody(z.q) }
        """
      }
      val blockName = ss.resolveExtractorBlockName(zs(0))
      val ext = s"""
      deepdive.extraction.extractors.${blockName} {
        sql: \"\"\" DROP TABLE IF EXISTS ${zs(0).q.head.name};
        CREATE TABLE ${zs(0).q.head.name} AS
        ${inputQueries.mkString(" UNION ")}
        \"\"\"
        style: "sql_extractor"
        ${ss.generateDependenciesOfCompiledBlockFor(zs)}
      }
    """
      List(ext)
    }
    if (ss.isQueryTerm(stmts(0).q.head.name))
      blocks :::= compileNodeRule(stmts, qs, ss)

    val inferenceRuleToCompileGroup    = stmts.groupBy(_.q.head.name)

    for (stmt <- stmts) {
      // edge query
      val fakeBody        = stmt.q.head +: stmt.q.body
      val fakeCQ          = ConjunctiveQuery(stmt.q.head, fakeBody) // we will just use the fakeBody below.

      val index = stmt.q.body.length + 1
      val qs2 = new QuerySchema( fakeCQ )
      val variableIdsStr = Some(s"""R0.id AS "${stmt.q.head.name}.R0.id" """)
      val variableColsStr = Some(s"""R0.label AS "${stmt.q.head.name}.R0.label" """)

      // weight string
      val uwStr = stmt.weights match {
        case KnownFactorWeight(x) => None
        case UnknownFactorWeight(w) => Some(w.flatMap(s => ss.resolveColumn(s, qs2, fakeCQ, true)).mkString(", "))
      }

      val selectStr = (List(variableIdsStr, variableColsStr, uwStr) flatMap (u => u)).mkString(", ")

      val ddCount = if (isIncremental) ( fakeCQ.body.zipWithIndex map { case(x,i) => s"R${i}.dd_count"}).mkString(" * ") else ""
      val ddCountStr = if (ddCount.length > 0) s""", ${ddCount} AS \"dd_count\" """ else ""

      // factor input query
      val inputQuery = s"""
        SELECT ${selectStr} ${ddCountStr}
        ${ ss.generateSQLBody(fakeCQ) }"""

      // factor function
      val func = s"""Imply(${stmt.q.head.name}.R0.label)"""

      // weight
      val weight = stmt.weights match {
        case KnownFactorWeight(x) => s"${x}"
        case UnknownFactorWeight(w) => {
          s"""?(${w.flatMap(s => ss.resolveColumn(s, qs2, fakeCQ, false)).mkString(", ")})"""
        }
      }

      blocks ::= s"""
        deepdive.inference.factors.factor_${stmt.q.head.name} {
          input_query: \"\"\"${inputQuery}\"\"\"
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

  // generate variable schema statements
  def compileVariableSchema(statements: DeepDiveLog.Program, ss: CompilationState): CompiledBlocks = {
    var schema = Set[String]()
    // generate the statements.
    statements.foreach {
      case InferenceRule(q, weights, supervision) =>
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
      // derive and compile the program with delta rules instead for incremental version
      if (config.isIncremental) DeepDiveLogDeltaDeriver.derive(parsedProgram)
      else parsedProgram

    // take an initial pass to analyze the parsed program
    val state = new CompilationState( programToCompile )


    val extractionRuleToCompile = new ListBuffer[ExtractionRule]()
    val inferenceRuleToCompile = new ListBuffer[InferenceRule]()
    val functionCallRuleToCompile = new ListBuffer[FunctionCallRule]()
    programToCompile foreach (_ match {
      case s: ExtractionRule   => extractionRuleToCompile += s
      case s: FunctionCallRule => functionCallRuleToCompile += s
      case s: InferenceRule    => inferenceRuleToCompile += s
      case _                   => 
    })

    val extractionRuleToCompileGroup   = extractionRuleToCompile.groupBy(_.q.head.name)
    val inferenceRuleToCompileGroup    = inferenceRuleToCompile.groupBy(_.q.head.name)
    val functionCallRuleToCompileGroup = functionCallRuleToCompile.groupBy(_.input)

    val body = new ListBuffer[String]()
    extractionRuleToCompileGroup   foreach {keyVal => body ++= compileE(keyVal._2.toList, state, config.isIncremental)}
    functionCallRuleToCompileGroup foreach {keyVal => body ++= compileF(keyVal._2.toList, state, config.isIncremental)}
    inferenceRuleToCompileGroup    foreach {keyVal => body ++= compileI(keyVal._2.toList, state, config.isIncremental)}
    
    // compile the program into blocks of application.conf
    val blocks = (
      compileUserSettings
      :::
      compileVariableSchema(programToCompile, state)
      :::
      body.toList 
    )

    // emit the generated code
    blocks foreach println

    if (config.isIncremental) {
      // TODO emit extra extractor for moving rows of dd_delta_* to *
    }
  }
}
