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
import scala.language.postfixOps

object AliasStyle extends Enumeration {
  type AliasStyle = Value
  // TableStyle: tableName.R{tableIndex}.columnName
  // ViewStyle: column_{columnIndex}
  val ViewAlias, TableAlias, NoAlias, UseVariableAsAlias = Value
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

  // map relation name -> variable type
  var variableType : Map[String, VariableType]   = new HashMap[String, VariableType]()

  var function_schema : Map[String, FunctionDeclaration] = new HashMap[ String, FunctionDeclaration]()

  // The dependency graph between statements.
  var dependencies : Map[Statement, Set[Statement]] = new HashMap()

  // The statement whether will compile or union to other statements
  var visible : Set[Statement] = Set()

  var variableTableNames : Set[String] = Set()

  var mode : Mode = ORIGINAL

  // Mapping head names to the actual statements
  var schemaDeclarationGroupByHead  : Map[String, List[SchemaDeclaration]] = new HashMap[String, List[SchemaDeclaration]]()
  var extractionRuleGroupByHead     : Map[String, List[ExtractionRule]] = new HashMap[String, List[ExtractionRule]]()
  var functionCallRuleGroupByOutput : Map[String, List[FunctionCallRule]] = new HashMap[String, List[FunctionCallRule]]()
  var functionCallList              : List[FunctionCallRule] = Nil
  var inferenceRules                : List[InferenceRule] = Nil

  def init() = {
    // generate the statements.
    mode = config.mode
    statements.foreach {
      case decl: SchemaDeclaration => {
        val r = decl.a.name
        decl.a.terms.zipWithIndex.foreach {
          case (n, i) =>
            schema           += { (r, i) -> n }
            ground_relations += { r -> !decl.isQuery } // record whether a query or a ground term.
            if (decl.isQuery) variableType += { r -> decl.variableType.get }
        }
        if (decl.isQuery) variableTableNames += r
      }
      case fdecl : FunctionDeclaration => function_schema += {fdecl.functionName -> fdecl}
      case _ =>
    }
    functionCallList = statements collect { case s: FunctionCallRule => s }
    inferenceRules   = statements collect { case s: InferenceRule    => s }
    groupByHead(statements)
    analyzeVisible(statements)
    analyzeDependency(statements)
  }

  init()

  def error(message: String) {
    throw new RuntimeException(message)
  }

  def optionalIndex(idx: Int) =
    if (idx < 1) "" else s"${idx}"

  // Given a statement, resolve its name for the compiled extractor block.
  def resolveExtractorBlockName(s: Statement): String = s match {
    case d: SchemaDeclaration => s"init_${d.a.name}"
    case e: ExtractionRule => s"ext_${e.headName}"
    case f: FunctionCallRule => s"ext${
      // Extra numbering is necessary because there can be many function calls to the same head relation
      // XXX This comes right after the prefix to prevent potential collision with user's name
      val functionCallsForSameOutputAndFunction =
        (functionCallRuleGroupByOutput getOrElse(f.output, Set.empty) toList).filter (_.function == f.function)
      val idx = functionCallsForSameOutputAndFunction indexOf f
      optionalIndex(idx)
    }_${f.output}_by_${f.function}"
    case _ => sys.error(s"${s}: Cannot determine extractor block name for rule")
  }

  // Given an inference rule, resolve its name for the compiled inference block.
  def resolveInferenceBlockName(s: InferenceRule): String = {
    val factorFuncName: String = s.head.function.toString.toLowerCase
    val idxInInferenceRulesSharingHead = inferenceRules filter(_.head equals s.head) indexOf(s)
    mode match {
      case INCREMENTAL => s"dd_delta_inf${
        // XXX This comes right after the prefix to prevent potential collision with user's name
        optionalIndex(idxInInferenceRulesSharingHead)
      }_${
        factorFuncName
      }_${
        s.head.terms map {_.name stripPrefix("dd_new_")} mkString("_")
      }"
      case _ =>           s"inf${
        // XXX This comes right after the prefix to prevent potential collision with user's name
        optionalIndex(idxInInferenceRulesSharingHead)
      }_${
        // function name
        factorFuncName
      }_${
        // followed by variable names
        s.head.terms map {_.name} mkString("_")
      }"
    }
  }

  // Given a variable, resolve it.  TODO: This should give a warning,
  // if we encouter a variable that is not in this map, then something
  // odd has happened.
  def resolveName( v : Variable ) : String = {
    v match { case Variable(v,relName,i) =>
      if(schema contains (relName,i)) {
        schema(relName,i)
      } else {
        // for views, columns names are in the form of "column_index"
        return if (DeepDiveLogCompiler.isForIncremental(relName)) v else s"column_${i}"
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

  // has schema declared
  def hasSchemaDeclared(relName: String) : Boolean = {
    schemaDeclarationGroupByHead contains relName
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

    schemaDeclarationGroupByHead = mode match {
      case INCREMENTAL => schemaDeclarationToCompile.toList.filter(_.a.name.startsWith("dd_")).groupBy(_.a.name)
      case _           => schemaDeclarationToCompile.toList.groupBy(_.a.name)
    }
    extractionRuleGroupByHead      = extractionRuleToCompile.toList.groupBy(_.headName)
    functionCallRuleGroupByOutput  = functionCallRuleToCompile.toList.groupBy(_.output)
  }

  // Analyze the block visibility among statements
  def analyzeVisible(statements: List[Statement]) = {
    extractionRuleGroupByHead.values foreach { visible += _(0) }
    functionCallList foreach { visible += _ }
  }

  // Analyze the dependency between statements and construct a graph.
  def analyzeDependency(statements: List[Statement]) = {
    val stmtByHeadName = (extractionRuleGroupByHead.toSeq ++
      functionCallRuleGroupByOutput.toSeq).groupBy(_._1).mapValues(_.map(_._2).toList)

    def collectUsedRelations(bodies: List[List[Body]]): Set[Statement] = {
      // FIXME need to be recursively defined on QuantifiedBody
      (bodies.flatten collect { case x: BodyAtom => x.name }
      flatMap (stmtByHeadName get _)).toSet.flatten.flatten
    }

    // Look at the body of each statement to construct a dependency graph
    statements foreach {
      case f : FunctionCallRule => dependencies += {
        f -> collectUsedRelations(f.q.bodies) }
      case e : ExtractionRule   => dependencies += {
        e -> collectUsedRelations(e.q.bodies) }
      case e : InferenceRule    => dependencies += {
        e -> collectUsedRelations(e.q.bodies) }
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

  def collectUsedRelations1(body: Body): List[String] = body match {
    case a: BodyAtom => List(a.name)
    case q: QuantifiedBody => collectUsedRelations(q.bodies)
    case _ => List.empty
  }
  def collectUsedRelations(bodies: List[Body]): List[String] = bodies flatMap collectUsedRelations1
  val relationNamesUsedByStatement = statements collect {
      case f : FunctionCallRule => f -> (f.q.bodies flatMap collectUsedRelations distinct)
      case e : ExtractionRule   => e -> (e.q.bodies flatMap collectUsedRelations distinct)
      case e : InferenceRule    => e -> (e.q.bodies flatMap collectUsedRelations distinct)
    } toMap
}

// This is responsible for compiling a single conjunctive query
class QueryCompiler(cq : ConjunctiveQuery, ss: CompilationState) {
  // This is responsible for schema elements within a given query, e.g.,
  // what is the canonical version of x? (i.e., the first time it is
  // mentioned in the body. This is useful to translate to SQL (join
  // conditions, select, etc.)
  var query_schema = new HashMap[ String, Tuple2[String,Variable] ]()

  // maps each variable name to a canonical version of itself (first occurence in body in left-to-right order)
  // index is the index of the subgoal/atom this variable is found in the body.
  // variable is the complete Variable type for the found variable.
  def generateCanonicalVar()  = {
    def generateCanonicalVarFromAtom(a: BodyAtom, index: String) {
      a.terms.zipWithIndex.foreach { case (expr, i) =>
        expr match {
          case VarPattern(v) =>
            if (! (query_schema contains v) )
              query_schema += { v -> (index, Variable(v,a.name,i) ) }
          case _ =>
        }
      }
    }
    // For quantified body, we need to recursively add variables from the atoms inside the quantified body
    def generateCanonicalVarFromBodies(bodies: List[Body], indexPrefix: String) {
      bodies.zipWithIndex.foreach {
        case (a: BodyAtom, index) => generateCanonicalVarFromAtom(a, s"${indexPrefix}${index}")
        // we need to recursively handle the atoms inside the modifier
        case (a: QuantifiedBody, index) => generateCanonicalVarFromBodies(a.bodies, s"${indexPrefix}${index}_")
        case _ =>
      }
    }

    cq.bodies foreach { generateCanonicalVarFromBodies(_, "") }
  }
  generateCanonicalVar() // initialize

  // accessors
  def getBodyIndex( varName : String )       = { query_schema(varName)._1 }
  def getVar(varName : String ) : Variable   = { query_schema(varName)._2 }

  // compile a variable name to its canonical relation and column in R{index}.{name} style
  def compileVariable(v: String) = {
    val index = getBodyIndex(v)
    val name  = ss.resolveName(getVar(v))
    s"R${index}.${name}"
  }

  // compile alias
  def compileAlias(e: Expr, aliasStyle: AliasStyle) = aliasStyle match {
    case NoAlias => ""
    case TableAlias => e match {
      case VarExpr(v) => {
        val index = getBodyIndex(v)
        val variable = getVar(v)
        val name  = ss.resolveName(variable)
        val relation = variable.relName
        s""" AS "${relation}.R${index}.${name}"""" //"
      }
      case _ => s" AS column_${cq.headTerms indexOf e}"
    }
    case ViewAlias => s" AS column_${cq.headTerms indexOf e}"
    case UseVariableAsAlias => s""" AS "${DeepDiveLogPrettyPrinter.print(e)}""""
  }

  // resolve an expression
  def compileExpr(e: Expr) : String = compileExpr(e, 0)
  def compileExpr(e: Expr, level: Int) : String = {
    e match {
      case VarExpr(name) => compileVariable(name)
      case NullConst() => "NULL"
      case IntConst(value) => value.toString
      case DoubleConst(value) => value.toString
      case BooleanConst(value) => value.toString
      case StringConst(value) => s"'${value.replaceAll("'", "''")}'"
      case FuncExpr(function, args, agg) => {
        val resolvedArgs = args map (x => compileExpr(x))
        val resolved = s"${function}(${resolvedArgs.mkString(", ")})"
        resolved
      }
      case BinaryOpExpr(lhs, op, rhs) => {
        val resovledLhs = compileExpr(lhs, level + 1)
        val resovledRhs = compileExpr(rhs, level + 1)
        val sql = s"${resovledLhs} ${op} ${resovledRhs}"
        if (level == 0) sql else s"(${sql})"
      }
      case TypecastExpr(lhs, rhs) => {
        val resovledLhs = compileExpr(lhs)
        s"(${resovledLhs} :: ${rhs})"
      }
      case IfThenElseExpr(ifCondThenExprPairs, optElseExpr) => {
        (ifCondThenExprPairs map {
          case (ifCond, thenExpr) => s"WHEN ${compileCond(ifCond)} THEN ${compileExpr(thenExpr)}"
        }) ++ List(optElseExpr map compileExpr mkString("ELSE ", "", ""))
      } mkString("\nCASE ", "\n     ", "\nEND")
    }
  }

  // resolve a condition
  def compileCond(cond: Cond) : String = compileCond(cond, 0)
  def compileCond(cond: Cond, level: Int) : String = {
    cond match {
      case ExprCond(e) => compileExpr(e)
      case NegationCond(c) => s"NOT ${compileCond(c, level + 1)}"
      case CompoundCond(lhs, op, rhs) => {
        val resolvedLhs = s"${compileCond(lhs, level + 1)}"
        val resolvedRhs = s"${compileCond(rhs, level + 1)}"
        val sql = op match {
          case LogicOperator.AND => s"${resolvedLhs} AND ${resolvedRhs}"
          case LogicOperator.OR  => s"${resolvedLhs} OR ${resolvedRhs}"
        }
        if (level == 0) sql else s"(${sql})"
      }
      case _ => ""
    }
  }

  // generate SQL SELECT cluase
  def generateSQLHead(aliasStyle: AliasStyle) = {
    val head = if (cq.headTerms isEmpty) "*" else cq.headTerms map { expr =>
      compileExpr(expr) + compileAlias(expr, aliasStyle)
    } mkString(", ")
    val distinctStr = if (cq.isDistinct) "DISTINCT " else ""
    s"${distinctStr}${head}"
  }

  // This is generic code that generates the FROM with positional aliasing R0, R1, etc.
  // and the corresponding WHERE clause
  def generateSQLBody(body: List[Body]) : String = {
    // generate where clause from unification and conditions
    def generateWhereClause(bodies: List[Body], indexPrefix: String) : String = {
      val conditions = bodies.zipWithIndex.flatMap {
        case (BodyAtom(relName, terms), index) => {
          val bodyIndex = s"${indexPrefix}${index}"
          terms.zipWithIndex flatMap { case (pattern, termIndex) =>
            pattern match {
              // a simple variable indicates a join condition with other columns having the same variable name
              case VarPattern(varName) => {
                val canonical_body_index = getBodyIndex(varName)
                if (canonical_body_index != bodyIndex) {
                  val real_attr_name1 = ss.resolveName( Variable(varName, relName, termIndex) )
                  val real_attr_name2 = ss.resolveName( getVar(varName))
                  Some(s"R${ bodyIndex }.${ real_attr_name1 } = R${ canonical_body_index }.${ real_attr_name2 } ")
                } else { None }
              }
              case PlaceholderPattern() => None
              // expression patterns indicate a filter condition on the column
              case ExprPattern(expr) => {
                val resolved = compileExpr(expr)
                val attr = ss.schema(relName, termIndex)
                Some(s"R${bodyIndex}.${attr} = ${resolved}")
              }
            }
          }
        }
        case (x: Cond, _) => Some(compileCond(x))
        case (x: QuantifiedBody, index) => {
          val newIndexPrefix = s"${indexPrefix}${index}_"
          val subqueryWhereStr = generateWhereClause(x.bodies, newIndexPrefix)
          val subqueryFromStr  = generateFromClause(x.bodies, newIndexPrefix)
          x.modifier match {
            case ExistModifier(negated) => Some(s"${if (negated) "NOT " else ""}EXISTS (SELECT 1 FROM ${subqueryFromStr} WHERE ${subqueryWhereStr})")
            case AllModifier() => Some(s"NOT EXISTS (SELECT 1 FROM ${subqueryFromStr} WHERE NOT (${subqueryWhereStr}))")
            case _ => None
          }
        }
      }
      conditions.mkString(" AND ")
    }

    def generateFromClause(bodies: List[Body], indexPrefix: String) : String = {
      val atoms = bodies.zipWithIndex flatMap {
        case (x:BodyAtom,i) => Some(s"${x.name} R${indexPrefix}${i}")
        case _ => None
      }
      val outers = bodies.zipWithIndex flatMap {
        case (x:QuantifiedBody,i) => {
          val newIndexPrefix = s"${indexPrefix}${i}_"
          x.modifier match {
            case OuterModifier() => {
              val from = generateFromClause(x.bodies, newIndexPrefix)
              val joinCond = generateWhereClause(x.bodies, newIndexPrefix)
              Some(s"${from} ON ${joinCond}")
            }
            case _ => None
          }
        }
        case _ => None
      }
      // full outer join
      if (atoms isEmpty) {
        outers.mkString(" FULL OUTER JOIN ")
      } else {
        (atoms.mkString(", ") +: outers).mkString(" LEFT OUTER JOIN ")
      }
    }

    def optionalClause(prefix: String, clause: String): String =
      if (clause.isEmpty) "" else prefix + " " + clause

    // check if an expression contains an aggregation function
    def containsAggregation(expr: Expr) : Boolean = {
      expr match {
        case FuncExpr(function, args, agg) => if (agg) agg else {
          args.map(containsAggregation).foldLeft(false)(_ || _)
        }
        case BinaryOpExpr(lhs, op, rhs) => containsAggregation(lhs) || containsAggregation(rhs)
        case TypecastExpr(lhs, rhs) => containsAggregation(lhs)
        case _ => false
      }
    }

    // handle group by
    // map head terms, leaving out aggregation functions
    val groupbyTerms = cq.headTerms.filterNot(containsAggregation) map (compileExpr(_))

    val groupbyStr = if (groupbyTerms.size == cq.headTerms.size || groupbyTerms.isEmpty) {
      ""
    } else {
      s"\n        GROUP BY ${groupbyTerms.mkString(", ")}"
    }

    // limit clause
    val limitStr = optionalClause("\nLIMIT", cq.limit.getOrElse("").toString)

    // order by clause by handling any @order_by annotations
    val orderbyStr =
      if (cq.headTermAnnotations.size != cq.headTerms.size) "" // invalid annotations
      else {
        val ORDER_BY_ANNOTATION_NAMES = Set("order_by")
        val orderByAnnos = cq.headTermAnnotations map {
          case annos => annos collectFirst {
            case anno if ORDER_BY_ANNOTATION_NAMES contains anno.name => anno
          }
        }
        val orderByExprs = cq.headTerms.zip(orderByAnnos).zipWithIndex collect {
          case ((e, Some(orderByAnno)), i) =>
            // TODO factor out annotation argument handling
            val isAscending = // the "dir" or first argument of the annotation decides the sort order, defaulting to ASCending
              orderByAnno.args collect {
                case Left(argsMap) => (argsMap getOrElse("dir", "ASC")).toString == "ASC"
                case Right(dir :: _) => dir.toString == "ASC"
              } getOrElse(true)
            val priority = // the priority or second argument decides the key index
              orderByAnno.args collect {
                case Left(argsMap) => (argsMap getOrElse("priority", i+1)).toString.toInt
                case Right(_ :: priority :: _) => priority.toString.toInt
              } getOrElse(i+1)
            (e, isAscending, priority)
        } sortBy { _._3 }
        optionalClause("\nORDER BY", orderByExprs map {
            case (e, isAscending, _) => s"${compileExpr(e)} ${if (isAscending) "ASC" else "DESC"}"
          } mkString(", ")
        )
      }

    s"""FROM ${ generateFromClause(body, "") }
        ${ optionalClause("WHERE", generateWhereClause(body, "")) }${groupbyStr}${orderbyStr}${limitStr}"""
  }

  def generateSQL(aliasStyle: AliasStyle = ViewAlias) = {
    val head = generateSQLHead(aliasStyle)
    val body = cq.bodies map generateSQLBody
    body map { b => s"SELECT ${head}\n${b}" } mkString("\nUNION ALL\n")
  }

}


// Compiler that takes parsed program as input and prints blocks of application.conf
object DeepDiveLogCompiler extends DeepDiveLogHandler {
  type CompiledBlock = String
  type CompiledBlocks = List[CompiledBlock]

  def isForIncremental(name: String) = name startsWith "dd_new_"

  def escape4sh(arg: String): String = s"'${arg.replaceAll("'", "'\\\\''")}'"

  // Generate schema and cleanup part for database
  def compileSchemaDeclarations(stmts: List[SchemaDeclaration], ss: CompilationState): CompiledBlocks = {
    var schemas = new ListBuffer[String]()
    for (stmt <- stmts) {
      if (isForIncremental(stmt.a.name) && (ss.isQueryTerm(stmt.a.name))) {
      } else {
        var columnDecls = stmt.a.terms.zipWithIndex map {
          case (name,i) => s"${name} ${stmt.a.types(i)}"
        }
        if (stmt.isQuery) {
          val labelColumn = stmt.variableType match {
            case Some(BooleanType)        => "label boolean"
            case Some(MultinomialType(_)) => "label int"
          }
          columnDecls = columnDecls :+ "id bigint" :+ labelColumn
        }
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
    }
    // Cleanup incremental table extractor
    val truncateTableList = (stmts map (x =>
      if (isForIncremental(x.a.name) && (ss.isQueryTerm(x.a.name))) ""
      else s"TRUNCATE ${x.a.name};")).filter(_ != "")
    if (truncateTableList.length > 0) {
      schemas += s"""
        deepdive.extraction.extractors.cleanup {
          sql: \"\"\"
          ${truncateTableList.mkString("\n          ")}
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
        val tmpCq = stmt.q.copy(bodies = List(cqBody))
        // Generate the body of the query.
        val qc              = new QueryCompiler(tmpCq, ss)

        if (stmt.supervision != None) {
          if (stmt.q.bodies.length > 1) ss.error(s"Scoping rule does not allow disjunction.\n")
          val headStr = qc.generateSQLHead(NoAlias)
          val labelCol = qc.compileExpr(stmt.supervision.get)
          inputQueries += s"""SELECT DISTINCT ${ headStr }, 0 AS id, ${labelCol} AS label
          ${ qc.generateSQLBody(cqBody) }
          """
        } else if ((ss.hasSchemaDeclared(stmt.headName)) &&
          (ss.schemaDeclarationGroupByHead(stmt.headName)(0).isQuery) &&
          isForIncremental(stmt.headName)) {
          val headStr = qc.generateSQLHead(NoAlias)
          inputQueries += s"""SELECT DISTINCT ${ headStr }, id, label
          ${ qc.generateSQLBody(cqBody) }
          """
        } else {
          // variable columns
          // dd_new_ tale only need original column name to make sure the schema is the same with original table
          // views uses view alias
          val aliasStyle = ss.mode match {
            case MERGE => NoAlias
            case _     => if (isForIncremental(stmt.headName)) NoAlias
              else if (!ss.hasSchemaDeclared(stmt.headName)) ViewAlias
              else TableAlias
          }
          inputQueries += qc.generateSQL(aliasStyle)
        }
      }
    }
    val blockName = ss.resolveExtractorBlockName(stmts(0))
    val sqlCmdForCleanUp = ss.mode match {
      case MERGE => s"TRUNCATE ${stmts(0).headName};"
      case _ => if (ss.hasSchemaDeclared(stmts(0).headName)) {
        if (isForIncremental(stmts(0).headName) && !(ss.isQueryTerm(stmts(0).headName)))
          s"TRUNCATE ${stmts(0).headName};"
        else ""
      } else s"DROP VIEW IF EXISTS ${stmts(0).headName} CASCADE;"
    }
    val createTable = ss.mode match {
      case MERGE => true
      case _ => (!isForIncremental(stmts(0).headName) &&
        (ss.hasSchemaDeclared(stmts(0).headName))) ||
        (isForIncremental(stmts(0).headName) &&
        !(ss.isQueryTerm(stmts(0).headName)))
    }
    val extractor = s"""
      deepdive.extraction.extractors.${blockName} {
        cmd: \"\"\"
${if (createTable) {
	s"""
	# TODO use temporary table
	deepdive create table "${stmts(0).headName}"
	deepdive sql ${escape4sh(s"INSERT INTO ${stmts(0).headName} ${inputQueries.mkString("\nUNION ALL\n")}")}
	# TODO rename temporary table to replace output_relation
	"""
} else {
	s"""
	deepdive create view ${stmts(0).headName} as ${escape4sh(inputQueries.mkString("\nUNION ALL\n"))}
	"""
}}
        \"\"\"
          output_relation: \"${stmts(0).headName}\"
        style: "cmd_extractor"
          ${ss.generateDependenciesOfCompiledBlockFor(stmts)}
          input_relations: [${
            (stmts flatMap ss.relationNamesUsedByStatement distinct
            ) mkString("\n            ", "\n            ", "\n          ")}]
      }
    """
    List(extractor)
  }

  def compileFunctionCallRules(stmts: List[FunctionCallRule], ss: CompilationState): CompiledBlocks = {
    var extractors = new ListBuffer[String]()
    for (stmt <- stmts) {

      val function = ss.resolveFunctionName(stmt.function)
      val udfDetails = (function.implementations collectFirst {
        case impl: RowWiseLineHandler =>
          s"""udf: $${APP_HOME}\"/${StringEscapeUtils.escapeJava(impl.command)}\"
          style: \"${impl.style}_extractor\" """
      })

      if (udfDetails.isEmpty)
        ss.error(s"Cannot find compilable implementation for function ${stmt.function} among:\n  "
          + (function.implementations mkString "\n  "))

      val blockName = ss.resolveExtractorBlockName(stmt)
      val parallelism = stmt.parallelism getOrElse("${PARALLELISM}")
      val extractor = s"""
        deepdive.extraction.extractors.${blockName} {
          input: \"\"\" ${new QueryCompiler(stmt.q, ss).generateSQL(TableAlias)}
          \"\"\"
          output_relation: \"${stmt.output}\"
          ${udfDetails.get}
          ${ss.generateDependenciesOfCompiledBlockFor(List(stmt))}
          input_relations: [${
            (List(stmt) flatMap ss.relationNamesUsedByStatement distinct
            ) mkString("\n            ", "\n            ", "\n          ")}]
          input_batch_size: $${INPUT_BATCH_SIZE}
          parallelism: ${parallelism}
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
        // Here we need to select from the bodies atoms as well as the head atoms,
        // which are the variable relations.
        // This is achieved by puting head atoms into the body.
        val headAsBody = stmt.head.terms map { x =>
          BodyAtom(x.name, x.terms map {
            case x: VarExpr => VarPattern(x.name)
            case x: Expr    => ExprPattern(x)
          })
        }
        val fakeBody        = headAsBody ++ cqBody
        val fakeCQ          = stmt.q.copy(bodies = List(fakeBody))
        val fakeBodyAtoms   = fakeBody.collect { case x: BodyAtom => x }

        val index = cqBody.length + 1
        val qc = new QueryCompiler(fakeCQ, ss)
        // TODO self-join?
        val varInBody = (fakeBody.zipWithIndex flatMap {
          case (x: BodyAtom, i) =>
            if (ss.variableTableNames contains x.name)
              Some(s"""R${fakeBody indexOf x}.id AS "${x.name}.R${fakeBody indexOf x}.id" """)
            else
              None
          case _ => None
        }) mkString(", ")

        // weight string
        val uwStr = stmt.weights.variables.zipWithIndex.flatMap {
          case(s: ConstExpr, i) => None
          case(s: Expr, i) => Some(qc.compileExpr(s) + s""" AS "dd_weight_column_${i}" """)
        } mkString(", ")

        val selectStr = List(varInBody, uwStr).filterNot(_.isEmpty).mkString(", ")

        // factor input query
        inputQueries += s"""
          SELECT ${selectStr}
          ${ qc.generateSQLBody(fakeBody) }"""
        // factor function
        if (func.length == 0) {
          val funcBody = (fakeBodyAtoms map {x =>
          if (ss.variableTableNames contains x.name)
            s"""${x.name}.R${fakeBody indexOf x}.label"""
          else ""
          }).filter(_ != "")

          val function = stmt.head.function match {
            case FactorFunction.Imply  => "Imply"
            case FactorFunction.And    => "And"
            case FactorFunction.Or     => "Or"
            case FactorFunction.Equal  => "Equal"
            case FactorFunction.Linear => "Linear"
            case FactorFunction.Ratio  => "Ratio"
            case FactorFunction.IsTrue => ss.variableType get stmt.head.terms(0).name match {
              case Some(BooleanType)        => "Imply"
              case Some(MultinomialType(_)) => "Multinomial"
            }
            case FactorFunction.Multinomial => "Multinomial"
          }
          func = s"""${function}(${funcBody.mkString(", ")})"""
        }
        // weight
        if (weight.length == 0) {
          // note error cases should be handled in semantic checker
          weight = stmt.weights.variables(0) match {
            case IntConst(value) => value.toString
            case DoubleConst(value) => value.toString
            case StringConst(value) => "?"
            case _ => {
              val weightVars = stmt.weights.variables.zipWithIndex.flatMap {
                case(s: Expr, i) => Some(s"dd_weight_column_${i}")
              } mkString(", ")
              s"?(${weightVars})"
            }
          }
        }
      }
      val blockName = ss.resolveInferenceBlockName(stmt)
      blocks ::= s"""
        deepdive.inference.factors.${blockName} {
          input_query: \"\"\"${inputQueries.mkString(" UNION ALL ")}\"\"\"
          function: "${func}"
          weight: "${weight}"
          ${ss.generateDependenciesOfCompiledBlockFor(stmts)}
          input_relations: [${
            val relationsInHead = stmts flatMap (_.head.terms map (_.name))
            val relationsInBody = stmts flatMap ss.relationNamesUsedByStatement
            ((relationsInHead ++ relationsInBody) distinct
            ) mkString("\n            ", "\n            ", "\n          ")}]
        }
      """
    }
    blocks.reverse
  }


  def compileUserSettings(ss: CompilationState): CompiledBlocks = {
    // TODO read user's proto-application.conf and augment it
    val mode = ss.mode match {
      case ORIGINAL        => "ORIGINAL"
      case INCREMENTAL     => "INCREMENTAL"
      case MATERIALIZATION => "MATERIALIZATION"
      case MERGE           => "MERGE"
    }
    List("""
  deepdive.db.default {
    driver: "org.postgresql.Driver"
    url: "jdbc:postgresql://"${PGHOST}":"${PGPORT}"/"${DBNAME}
    user: ${PGUSER}
    password: ${PGPASSWORD}
    dbname: ${DBNAME}
    host: ${PGHOST}
    port: ${PGPORT}
    incremental_mode: """ + s"""${mode}
    }
    """)
  }

  def compileVariableKey(ss: CompilationState): CompiledBlocks = {
    var keys = new ListBuffer[String]()
    for (stmt <- (ss.schemaDeclarationGroupByHead map (_._2)).flatten) {
      if (stmt.isQuery) keys += s"""${stmt.a.name} : [${stmt.a.terms.mkString(", ")}]"""
    }
    ss.mode match {
      case INCREMENTAL => List(s"""
      deepdive.schema.keys {
        ${keys.mkString("\n        ")}
      }""")
      case _ => List("")
    }
  }

  // generate application.conf pipelines
  def compilePipelines(ss: CompilationState): CompiledBlocks = {
    val run = "deepdive.pipeline.run: ${PIPELINE}"
    val extraction = (ss.visible map {s => ss.resolveExtractorBlockName(s)}).mkString("\n  ")
    val extraction_pipeline = if (extraction.length > 0) s"deepdive.pipeline.pipelines.extraction: [\n  ${extraction}\n]" else ""
    val inference = (ss.inferenceRules map {s => ss.resolveInferenceBlockName(s)}).mkString("\n  ")
    val inference_pipeline = if (inference.length > 0) s"deepdive.pipeline.pipelines.inference: [\n  ${inference}\n]" else ""
    val endtoend = List(extraction, inference).filter(_ != "").mkString("\n  ")
    val endtoend_pipeline = if (endtoend.length > 0) s"deepdive.pipeline.pipelines.endtoend: [\n  ${endtoend}\n]" else ""
    val base_dir = ss.mode match {
      case MATERIALIZATION | INCREMENTAL => "deepdive.pipeline.base_dir: ${BASEDIR}"
      case _ => ""
    }
    List(run, extraction_pipeline, inference_pipeline, endtoend_pipeline, base_dir).filter(_ != "") ++ (
    // FIXME remove the following after fully supporting incremental uops
    // XXX initdb pipeline and CREATE TABLE extractors has been moved to deepdive
    ss.mode match {
      case INCREMENTAL =>
        val setup_database_pipeline = ((ss.schemaDeclarationGroupByHead map (_._2)).flatten map {s => ss.resolveExtractorBlockName(s)}).mkString("\n  ")
        val initdb = if (setup_database_pipeline.length > 0) s"deepdive.pipeline.pipelines.initdb: [\n  ${setup_database_pipeline}\n]" else ""
        val cleanup_pipeline = ss.mode match {
          case INCREMENTAL | ORIGINAL => if (setup_database_pipeline.length > 0) s"deepdive.pipeline.pipelines.cleanup: [\n  cleanup\n]" else ""
          case _ => ""
        }
        List(initdb, cleanup_pipeline)
      case _ => List.empty
    }) // FIXME remove above after fully supporting incremental uops
  }

  // generate variable schema statements
  def compileVariableSchema(statements: DeepDiveLog.Program, ss: CompilationState): CompiledBlocks = {
    var schema = Set[String]()
    // generate the statements.
    statements.foreach {
      case decl: SchemaDeclaration =>
        if (decl.isQuery) {
          val variableTypeDecl = decl.variableType match {
            case Some(BooleanType)        => "Boolean"
            case Some(MultinomialType(x)) => s"Categorical(${x})"
          }
          schema += s"${decl.a.name}.label: ${variableTypeDecl}"
        }
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
    // semantic checking
    DeepDiveLogSemanticChecker.run(parsedProgram, config)
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
    config.mode match {
      case INCREMENTAL =>
        body ++= compileSchemaDeclarations((state.schemaDeclarationGroupByHead map (_._2)).flatten.toList, state)
      case _ =>
    }
    state.extractionRuleGroupByHead foreach {keyVal => body ++= compileExtractionRules(keyVal._2, state)}
    state.functionCallList          foreach {func   => body ++= compileFunctionCallRules(List(func), state)}
    state.inferenceRules            foreach {inf    => body ++= compileInferenceRules(List(inf), state)}

    // compile the program into blocks of application.conf
    val blocks = (
      compileUserSettings(state)
      :::
      compileVariableKey(state)
      :::
      compileVariableSchema(programToCompile, state)
      :::
      body.toList
      :::
      compilePipelines(state)
    )

    // emit the generated code
    blocks foreach println
  }
}
