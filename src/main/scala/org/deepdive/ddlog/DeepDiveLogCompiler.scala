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
  var inferenceRuleGroupByHead      : Map[String, List[InferenceRule]] = new HashMap[String, List[InferenceRule]]()
  var functionCallRuleGroupByInput  : Map[String, List[FunctionCallRule]] = new HashMap[String, List[FunctionCallRule]]()
  var functionCallRuleGroupByOutput : Map[String, List[FunctionCallRule]] = new HashMap[String, List[FunctionCallRule]]()
  var functionCallList              : ListBuffer[FunctionCallRule] = new ListBuffer[FunctionCallRule]()

  def init() = {
    // generate the statements.
    mode = config.mode
    statements.foreach {
      case SchemaDeclaration(Attribute(r, terms, types), isQuery, vType) => {
        terms.zipWithIndex.foreach {
          case (n, i) =>
            schema           += { (r,i) -> n }
            ground_relations += { r -> !isQuery } // record whether a query or a ground term.
            if (isQuery) variableType += { r -> vType.get }
        }
        if (isQuery) variableTableNames += r
      }
      case ExtractionRule(_,_) => ()
      case InferenceRule(_,_,_,_) => ()
      case fdecl : FunctionDeclaration => function_schema += {fdecl.functionName -> fdecl}
      case f: FunctionCallRule => functionCallList += f
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
    s"${s.q.head.name}_${(inferenceRuleGroupByHead map (_._2)).flatten.toList indexOf s}"
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
  def resolveColumn(s: String, q : ConjunctiveQuery, alias: AliasStyle) : Option[String] = {
    val qs = new QuerySchema(q)
    val index = qs.getBodyIndex(s)
    val name  = resolveName(qs.getVar(s))
    // TODO better way to get relation
    val relation = q.bodies(0)(index) match {
      case x: Atom => x.name
      case _  => error("Wrong index")
    }
    alias match {
      case OriginalOnly => Some(s"R${index}.${name}")
      case AliasOnly => Some(s"${relation}.R${index}.${name}")
      case OriginalAndAlias => Some(s"""R${index}.${name} AS "${relation}.R${index}.${name}" """)
    }
  }

  // resolve an expression
  def compileExpr(e: Expr, cq: ConjunctiveQuery, alias: AliasStyle, index : Int, isHead: Boolean) : String = {
    // recursively compile an expression
    def compileExprInner(e: Expr, alias: AliasStyle) : String = {
      e match {
        case VarExpr(name) => resolveColumn(name, cq, alias).get
        case ConstExpr(value) => value
        case FuncExpr(function, args, agg) => {
          // note alias is overriden with OriginalOnly, because when an expression appears in
          // the function, we only need it's column name, without " AS ..." aliasing, same below
          val resolvedArgs = args map (x => compileExprInner(x, OriginalOnly))
          val resolved = s"${function}(${resolvedArgs.mkString(", ")})"
          resolved
        }
        case BinaryOpExpr(lhs, op, rhs) => {
          val resovledLhs = compileExprInner(lhs, OriginalOnly)
          val resovledRhs = compileExprInner(rhs, OriginalOnly)
          s"(${resovledLhs} ${op} ${resovledRhs})"
        }
        case TypecastExpr(lhs, rhs) => {
          val resovledLhs = compileExprInner(lhs, OriginalOnly)
          s"(${resovledLhs} :: ${rhs})"
        }
      }
    }
    // for columns without a name (constant, function call, binary operator), add an column index alias if necessary
    val columnAlias = e match {
      case x: VarExpr => ""
      case _ => if (isHead) s" AS column_${index}" else ""
    }
    compileExprInner(e, alias) + columnAlias
  }

  // resolve a condition
  def compileCond(cond: Cond, cq: ConjunctiveQuery) : String = {
    cond match {
      case ComparisonCond(lhs, op, rhs) =>
        s"${compileExpr(lhs, cq, OriginalOnly, 0, false)} ${op} ${compileExpr(rhs, cq, OriginalOnly, 0, false)}"
      case NegationCond(c) => s"(NOT ${compileCond(c, cq)})"
      case CompoundCond(lhs, op, rhs) => {
        val resolvedLhs = s"${compileCond(lhs, cq)}"
        val resolvedRhs = s"${compileCond(rhs, cq)}"
        op match {
          case LogicOperator.AND => s"(${resolvedLhs} AND ${resolvedRhs})"
          case LogicOperator.OR  => s"(${resolvedLhs} OR ${resolvedRhs})"
        }
      }
      case InCond(lhs, rhs) => {
        s"${compileExpr(lhs, cq, OriginalOnly, 0, false)} IN (SELECT * FROM ${rhs})"
      }
      case ExistCond(rhs) => s"EXISTS (SELECT * FROM ${rhs})"
      case _ => ""
    }
  }

  // This is generic code that generates the FROM with positional aliasing R0, R1, etc.
  // and the corresponding WHERE clause (equating all variables)
  def generateSQLBody(z : ConjunctiveQuery) : String = {
    // Simple logic for the where clause, first find every first occurence of a
    // and stick it in a map.
    val qs = new QuerySchema(z)

    var whereClause = z.bodies(0).zipWithIndex flatMap {
      case (Atom(relName, terms),bodyIndex) => {
        terms.zipWithIndex flatMap { case (expr, index) =>
          expr match {
            // a simple variable indicates a join condition with other columns having the same variable name
            case VarExpr(varName) => {
              val canonical_body_index = qs.getBodyIndex(varName)
              if (canonical_body_index != bodyIndex) {
                val real_attr_name1 = resolveName( Variable(varName, relName, index) )
                val real_attr_name2 = resolveName( qs.getVar(varName))
                Some(s"R${ bodyIndex }.${ real_attr_name1 } = R${ canonical_body_index }.${ real_attr_name2 } ")
              } else { None }
            }
            // other expressions indicate a filter condition on the column
            case _ => {
              val resolved = compileExpr(expr, z, OriginalOnly, index, false)
              val attr = schema(relName, index)
              Some(s"R${bodyIndex}.${attr} = ${resolved}")
            }
          }
        }
      }
      case (x: Cond, i) => Some(compileCond(x, z))
    }

    // check if an expression contains an aggregation function
    def containsAggregation(expr: Expr) : Boolean = {
      expr match {
        case VarExpr(name) => false
        case ConstExpr(value) => false
        case FuncExpr(function, args, agg) => if (agg) agg else {
          args.map(containsAggregation).foldLeft(false)(_ || _)
        }
        case BinaryOpExpr(lhs, op, rhs) => containsAggregation(lhs) || containsAggregation(rhs)
        case TypecastExpr(lhs, rhs) => containsAggregation(lhs)
      }
    }

    // handle group by
    // map head terms, leaving out aggregation functions
    val groupbyTerms = z.head.terms.zipWithIndex flatMap { case (expr, index) =>
      if (containsAggregation(expr)) None
      else Some(compileExpr(expr, z, OriginalOnly, index, false))
    }

    val groupbyStr = if (groupbyTerms.size == z.head.terms.size || groupbyTerms.isEmpty) {
      ""
    } else {
      s"\n        GROUP BY ${groupbyTerms.mkString(", ")}"
    }

    val whereClauseStr = if (whereClause.isEmpty) "" else whereClause.mkString("WHERE ", " AND ", "")

    val limitStr = z.limit match {
      case Some(s) => s" LIMIT ${s}"
      case None => ""
    }

    val fromBodyNames = (z.bodies(0).zipWithIndex flatMap {
      case(x:Atom,i) => Some(s"${x.name} R${i}")
      case _ => None
    }).mkString(", ")
    val fromClause = fromBodyNames

    s"""FROM ${ fromClause }
        ${ whereClauseStr }${groupbyStr}${limitStr}"""
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
    extractionRuleGroupByHead      = extractionRuleToCompile.toList.groupBy(_.q.head.name)
    inferenceRuleGroupByHead       = inferenceRuleToCompile.toList.groupBy(_.q.head.name)
    functionCallRuleGroupByInput   = functionCallRuleToCompile.toList.groupBy(_.input)
    functionCallRuleGroupByOutput  = functionCallRuleToCompile.toList.groupBy(_.output)
  }

  // Analyze the block visibility among statements
  def analyzeVisible(statements: List[Statement]) = {
    extractionRuleGroupByHead.values foreach { visible += _(0) }
    functionCallList foreach { visible += _ }
  }

  // Analyze the dependency between statements and construct a graph.
  def analyzeDependency(statements: List[Statement]) = {
    val stmtByHeadName = (extractionRuleGroupByHead.toSeq ++ inferenceRuleGroupByHead.toSeq ++
      functionCallRuleGroupByOutput.toSeq).groupBy(_._1).mapValues(_.map(_._2).toList)

    // Look at the body of each statement to construct a dependency graph
    statements foreach {
      case f : FunctionCallRule => dependencies += {
        f -> (( Some(f.input) flatMap (stmtByHeadName get _)).toSet.flatten.flatten) }
      case e : ExtractionRule   => dependencies += {
        e -> (((e.q.bodies.flatten collect { case x: Atom => x.name })
          flatMap (stmtByHeadName get _)).toSet.flatten.flatten) }
      case w : InferenceRule    => dependencies += {
        w -> (((w.q.bodies.flatten collect { case x: Atom => x.name })
          flatMap (stmtByHeadName get _)).toSet.flatten.flatten) }
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
        terms.zipWithIndex.foreach { case (expr, i) =>
          expr match {
            case VarExpr(v) =>
              if (! (query_schema contains v) )
                query_schema += { v -> (index, Variable(v,relName,i) ) }
            case _ =>
          }
        }
      }
      case _ =>
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

  // Generate schema and cleanup part for database
  def compileSchemaDeclarations(stmts: List[SchemaDeclaration], ss: CompilationState): CompiledBlocks = {
    var schemas = new ListBuffer[String]()
    for (stmt <- stmts) {
      if ((stmt.a.name startsWith "dd_new_") && (ss.inferenceRuleGroupByHead contains stmt.a.name)) {
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
      if ((x.a.name startsWith "dd_new_") && (ss.inferenceRuleGroupByHead contains x.a.name)) ""
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
        val qs              = new QuerySchema( tmpCq )

        if (stmt.supervision != null) {
          if (stmt.q.bodies.length > 1) ss.error(s"Scoping rule does not allow disjunction.\n")
          val headTerms = tmpCq.head.terms.zipWithIndex map { case (expr, index) =>
            ss.compileExpr(expr, tmpCq, OriginalOnly, index, true)
          }
          val index = qs.getBodyIndex(stmt.supervision)
          val name  = ss.resolveName(qs.getVar(stmt.supervision))
          val labelCol = s"R${index}.${name}"
          val headTermsStr = ( headTerms :+ "0 as id" ).mkString(", ")
          inputQueries += s"""SELECT DISTINCT ${ headTermsStr }, ${labelCol} AS label
          ${ ss.generateSQLBody(tmpCq) }
          """
        } else if ((ss.schemaDeclarationGroupByHead contains stmt.q.head.name) && (ss.schemaDeclarationGroupByHead(stmt.q.head.name)(0).isQuery) && (stmt.q.head.name startsWith "dd_new_")) {
          val headTerms = tmpCq.head.terms.zipWithIndex map { case (expr, index) =>
            ss.compileExpr(expr, tmpCq, OriginalOnly, index, true)
          }
          val headTermsStr = ( headTerms :+ "id" ).mkString(", ")
          inputQueries += s"""SELECT DISTINCT ${ headTermsStr }, label
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
          val variableCols = tmpCq.head.terms.zipWithIndex map { case (expr, index) =>
            ss.compileExpr(expr, tmpCq, resolveColumnFlag, index, true)
          }
          val selectStr = variableCols.mkString(", ")
          val distinctStr = if (tmpCq.isDistinct) "DISTINCT" else ""
          inputQueries += s"""
            SELECT ${distinctStr} ${selectStr}
            ${ ss.generateSQLBody(tmpCq) }"""
        }
      }
    }
    val blockName = ss.resolveExtractorBlockName(stmts(0))
    // val createTable = if (ss.schemaDeclarationGroupByHead contains stmts(0).q.head.name) true else false
    val sqlCmdForCleanUp = ss.mode match {
      case MERGE => s"TRUNCATE ${stmts(0).q.head.name};"
      case _ => if (ss.schemaDeclarationGroupByHead contains stmts(0).q.head.name) {
        if (stmts(0).q.head.name.startsWith("dd_new_") && !(ss.inferenceRuleGroupByHead contains stmts(0).q.head.name))
          s"TRUNCATE ${stmts(0).q.head.name};"
        else ""
      } else s"DROP VIEW IF EXISTS ${stmts(0).q.head.name};"
    }
    val createTable = ss.mode match {
      case MERGE => true
      case _ => if ((!(stmts(0).q.head.name startsWith "dd_new_") && (ss.schemaDeclarationGroupByHead contains stmts(0).q.head.name)) || ((stmts(0).q.head.name startsWith "dd_new_") && !(ss.inferenceRuleGroupByHead contains stmts(0).q.head.name))) true else false
    }
    val sqlCmdForInsert  = if (createTable) "INSERT INTO" else "CREATE VIEW"
    val useAS            = if (createTable) "" else " AS"
    val extractor = s"""
      deepdive.extraction.extractors.${blockName} {
        sql: \"\"\" ${sqlCmdForCleanUp}
        ${sqlCmdForInsert} ${stmts(0).q.head.name}${useAS} ${inputQueries.mkString(" UNION ALL ")}
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
          s"""udf: $${APP_HOME}\"/${StringEscapeUtils.escapeJava(impl.command)}\"
          style: \"${impl.style}_extractor\" """
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
        val fakeCQ          = stmt.q.copy(bodies = List(fakeBody))
        val fakeBodyAtoms   = fakeBody.collect { case x: Atom => x }

        val index = cqBody.length + 1
        val qs2 = new QuerySchema( fakeCQ )
        // TODO self-join?
        val varInBody = (fakeBody.zipWithIndex flatMap {
          case (x: Atom, i) =>
            if (ss.variableTableNames contains x.name)
              Some(s"""R${fakeBody indexOf x}.id AS "${x.name}.R${fakeBody indexOf x}.id" """)
            else
              None
          case _ => None
        })

        val variableIdsStr = Some(varInBody.mkString(", "))

        // weight string
        val uwStr = stmt.weights match {
          case UnknownFactorWeight(w) => Some(w.flatMap(s => ss.resolveColumn(s, fakeCQ, OriginalAndAlias)).mkString(", "))
          case _ => None
        }

        val selectStr = (List(variableIdsStr, uwStr) flatten).mkString(", ")

        // factor input query
        inputQueries += s"""
          SELECT ${selectStr}
          ${ ss.generateSQLBody(fakeCQ) }"""
        // factor function
        if (func.length == 0) {
          val funcBody = (fakeBodyAtoms map {x =>
          if (ss.variableTableNames contains x.name)
            s"""${x.name}.R${fakeBody indexOf x}.label"""
          else ""
          }).filter(_ != "")
          val firstFunc = funcBody(0)
          // if function is not specified, use Imply for boolean, and Multinomial for multinomial variables
          val function  = stmt.function match {
            case Some(f) => f
            case None => ss.variableType get stmt.q.head.name match {
              case Some(BooleanType)        => "Imply"
              case Some(MultinomialType(_)) => "Multinomial"
            }
          }
          func = s"""${function}(${(funcBody.tail :+ firstFunc).mkString(", ")})"""
        }
        // weight
        if (weight.length == 0)
          weight = stmt.weights match {
            case KnownFactorWeight(x) => s"${x}"
            case UnknownFactorWeight(w) => {
              val weightVar = w.flatMap(s => ss.resolveColumn(s, fakeCQ, AliasOnly)).mkString(", ")
              s"?(${weightVar})"
            }
            case UnknownFactorWeightBindingToConst(w) => "?"
          }
      }
      val blockName = ss.resolveInferenceBlockName(stmt)
      blocks ::= s"""
        deepdive.inference.factors.${blockName} {
          input_query: \"\"\"${inputQueries.mkString(" UNION ALL ")}\"\"\"
          function: "${func}"
          weight: "${weight}"
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
    val setup_database_pipeline = ((ss.schemaDeclarationGroupByHead map (_._2)).flatten map {s => ss.resolveExtractorBlockName(s)}).mkString(", ")
    val initdb = if (setup_database_pipeline.length > 0) s"deepdive.pipeline.pipelines.initdb: [${setup_database_pipeline}]" else ""
    val extraction = (ss.visible map {s => ss.resolveExtractorBlockName(s)}).mkString(", ")
    val extraction_pipeline = if (extraction.length > 0) s"deepdive.pipeline.pipelines.extraction: [${extraction}]" else ""
    val inference = ((ss.inferenceRuleGroupByHead map (_._2)).flatten map {s => ss.resolveInferenceBlockName(s)}).mkString(", ")
    val inference_pipeline = if (inference.length > 0) s"deepdive.pipeline.pipelines.inference: [${inference}]" else ""
    val endtoend = List(extraction, inference).filter(_ != "").mkString(", ")
    val endtoend_pipeline = if (endtoend.length > 0) s"deepdive.pipeline.pipelines.endtoend: [${endtoend}]" else ""
    val cleanup_pipeline = ss.mode match {
      case INCREMENTAL | ORIGINAL => if (setup_database_pipeline.length > 0) s"deepdive.pipeline.pipelines.cleanup: [cleanup]" else ""
      case _ => ""
    }
    val base_dir = ss.mode match {
      case MATERIALIZATION | INCREMENTAL => "deepdive.pipeline.base_dir: ${BASEDIR}"
      case _ => ""
    }
    List(run, initdb, extraction_pipeline, inference_pipeline, endtoend_pipeline, cleanup_pipeline, base_dir).filter(_ != "")
  }

  // generate variable schema statements
  def compileVariableSchema(statements: DeepDiveLog.Program, ss: CompilationState): CompiledBlocks = {
    var schema = Set[String]()
    // generate the statements.
    statements.foreach {
      case SchemaDeclaration(a, isQuery, variableType) =>
        if (isQuery) {
          val variableTypeDecl = variableType match {
            case Some(BooleanType)        => "Boolean"
            case Some(MultinomialType(x)) => s"Categorical(${x})"
          }
          schema += s"${a.name}.label: ${variableTypeDecl}"
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
    body ++= compileSchemaDeclarations((state.schemaDeclarationGroupByHead map (_._2)).flatten.toList, state)
    state.extractionRuleGroupByHead    foreach {keyVal => body ++= compileExtractionRules(keyVal._2, state)}
    state.functionCallRuleGroupByInput foreach {keyVal => body ++= compileFunctionCallRules(keyVal._2, state)}
    state.inferenceRuleGroupByHead     foreach {keyVal => body ++= compileInferenceRules(keyVal._2, state)}

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
