import scala.util.parsing.combinator._
import scala.collection.immutable.HashMap
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

// ***************************************
// * The union types for for the parser. *
// ***************************************
trait Statement
case class Variable(varName : String, relName : String, index : Int )
case class Atom(name : String, terms : List[Variable])
case class Attribute(name : String, terms : List[Variable], types : List[String])
case class ConjunctiveQuery(head: Atom, body: List[Atom])
case class Column(name : String, t : String)

sealed trait FactorWeight {
  def variables : List[String]
}

case class KnownFactorWeight(value: Double) extends FactorWeight {
  def variables = Nil
}

case class UnknownFactorWeight(variables: List[String]) extends FactorWeight

case class SchemaElement( a : Attribute , query : Boolean ) extends Statement // atom and whether this is a query relation.
case class FunctionElement( functionName: String, input: String, output: String, implementation: String, mode: String) extends Statement
case class ExtractionRule(q : ConjunctiveQuery) extends Statement // Extraction rule
case class FunctionRule(input : String, output : String, function : String) extends Statement // Extraction rule
case class InferenceRule(q : ConjunctiveQuery, weights : FactorWeight, supervision : String) extends Statement // Weighted rule


// Parser
class ConjunctiveQueryParser extends JavaTokenParsers {
  // Odd definitions, but we'll keep them.
  // def stringliteral1: Parser[String] = ("'"+"""([^'\p{Cntrl}\\]|\\[\\"'bfnrt]|\\u[a-fA-F0-9]{4})*"""+"'").r ^^ {case (x) => x}
  // def stringliteral2: Parser[String] = """[a-zA-Z_0-9\./]*""".r ^^ {case (x) => x}
  // def stringliteral: Parser[String] = (stringliteral1 | stringliteral2) ^^ {case (x) => x}
  def stringliteral: Parser[String] = """[a-zA-Z0-9_\[\]]+""".r
  def path: Parser[String] = """[a-zA-Z0-9\./_]+""".r

  // relation names and columns are just strings.
  def relation_name: Parser[String] = stringliteral ^^ {case (x) => x}
  def col : Parser[String] = stringliteral  ^^ { case(x) => x }
  def attr : Parser[Column] = stringliteral ~ stringliteral ^^ {
    case(x ~ y) => Column(x, y)
  }

  def atom: Parser[Atom] = relation_name ~ "(" ~ rep1sep(col, ",") ~ ")" ^^ {
    case (r ~ "(" ~ cols ~ ")") => {
      val vars = cols.zipWithIndex map { case(name,i) => Variable(name, r, i) }
      Atom(r,vars)
    }
  }

  def attribute: Parser[Attribute] = relation_name ~ "(" ~ rep1sep(attr, ",") ~ ")" ^^ {
    case (r ~ "(" ~ attrs ~ ")") => {
      val vars = attrs.zipWithIndex map { case(x, i) => Variable(x.name, r, i) }
      var types = attrs map { case(x) => x.t }
      Attribute(r,vars, types)
    }
  }

  def udf : Parser[String] = stringliteral ^^ {case (x) => x}

  def query : Parser[ConjunctiveQuery] = atom ~ ":-" ~ rep1sep(atom, ",") ^^ {
    case (headatom ~ ":-" ~ bodyatoms) => ConjunctiveQuery(headatom, bodyatoms.toList)
  }

  def schemaElement : Parser[SchemaElement] = attribute ~ opt("?") ^^ {
    case (a ~ None) => SchemaElement(a,true)
    case (a ~ Some(_)) =>  SchemaElement(a,false)
  }


  def functionElement : Parser[FunctionElement] = "function" ~ stringliteral ~
  "over like" ~ stringliteral ~ "returns like" ~ stringliteral ~ "implementation" ~
  "\"" ~ path ~ "\"" ~ "handles" ~ stringliteral ~ "lines" ^^ {
    case ("function" ~ a ~ "over like" ~ b ~ "returns like" ~ c ~ "implementation" ~
      "\"" ~ d ~ "\"" ~ "handles" ~ e ~ "lines") => FunctionElement(a, b, c, d, e)
  }

  def extractionRule : Parser[ExtractionRule] = query  ^^ {
    case (q) => ExtractionRule(q)
    // case (q ~ "udf" ~ "=" ~ None)       => ExtractionRule(q,None)
  }

  def functionRule : Parser[FunctionRule] = stringliteral ~ ":-" ~ "!" ~ stringliteral ~ "(" ~ stringliteral ~ ")" ^^ {
    case (a ~ ":-" ~ "!" ~ b ~ "(" ~ c ~ ")") => FunctionRule(c, a, b)
  }

  def constantWeight = "weight" ~> "=" ~> """-?[\d\.]+""".r ^^ { x => KnownFactorWeight(x.toDouble) }
  def unknwonWeight = "weight" ~> "=" ~> opt(rep1sep(col, ",")) ^^ {
    case Some(varList) => UnknownFactorWeight(varList.toList)
    case _ => UnknownFactorWeight(List())
  }
  def factorWeight = constantWeight | unknwonWeight

  def supervision = "label" ~> "=" ~> col

  def inferenceRule : Parser[InferenceRule] = query ~ factorWeight ~ supervision ^^ {
    case (q ~ weight ~ supervision) => InferenceRule(q, weight, supervision)
  }

  // rules or schema elements in aribitrary order
  def statement : Parser[Statement] = (functionElement | inferenceRule | extractionRule | functionRule | schemaElement) ^^ {case(x) => x}

  type Program = List[Statement]
  def statements : Parser[Program] = rep1sep(statement, ".") ^^ { case(x) => x }
}

// This handles the schema statements.
// It can tell you if a predicate is a "query" predicate or a "ground prediate"
// and it resolves Variables their correct and true name in the schema, i.e. R(x,y) then x could be Attribute1 declared.
class CompilationState( statements : List[Statement] )  {
    // TODO: refactor the schema into a class that constructs and
    // manages these maps. Also it should have appropriate
    // abstractions and error handling for missing values.
    // ** Start refactor.
  var schema : Map[ Tuple2[String,Int], String ] = new HashMap[ Tuple2[String,Int], String ]()

  var ground_relations : Map[ String, Boolean ]  = new HashMap[ String, Boolean ]()

  var function_schema : Map[String, FunctionElement] = new HashMap[ String, FunctionElement]()

  var headNames : List[(Int, String)] = List()

  def init() = {
    // generate the statements.
    statements.foreach {
      case SchemaElement(Attribute(r, terms, types),query) =>
        terms.foreach {
          case Variable(n,r,i) =>
            schema           += { (r,i) -> n }
            ground_relations += { r -> query } // record whether a query or a ground term.
        }
      case ExtractionRule(_) => ()
      case InferenceRule(_,_,_) => ()
      case FunctionElement(a, b, c, d, e) => function_schema += {a -> FunctionElement(a, b, c, d, e)}
      case FunctionRule(_,_,_) => ()
    }

    // TODO analyze the real dependency (Map[headname, List[headname]]) here
    headNames = statements.zipWithIndex flatMap {
      case (e : ExtractionRule, i) => Some(i, e.q.head.name)
      case (f : FunctionRule  , i) => Some(i, f.output     )
      case (w : InferenceRule , i) => Some(i, w.q.head.name)
      case _ => None
    }

  }

  init()

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

  def resolveFunctionName( v : String ) : FunctionElement = {
    if (function_schema contains v) {
      function_schema(v)
    } else {
      return FunctionElement("0","0","0","0","0")
    }

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

// The compiler
object DeepDiveLogCompiler {

  def parseArgs(args: Array[String]) = {
    val getContents = (filename: String) => {
      val source = scala.io.Source.fromFile(filename)
      try source.getLines mkString "\n" finally source.close()
    }
    args.map(getContents).reduce(_ ++ _)
  }

  val parser = new ConjunctiveQueryParser
  def parseProgram(inputProgram: String) = parser.parse(parser.statements, inputProgram)

  type CompiledBlocks = List[String]

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
  def compileVariableSchema(statements: List[Statement], ss: CompilationState): CompiledBlocks = {
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

  // Generate extraction rule part for deepdive
  def compile(r: ExtractionRule, index: Int, ss: CompilationState): CompiledBlocks = {
    // Generate the body of the query.
    val qs              = new QuerySchema( r.q )
    // variable columns
    val variableCols = r.q.head.terms flatMap {
      case(Variable(v,rr,i)) => ss.resolveColumn(v, qs, r.q, true)
    }

    val variableColsStr = if (variableCols.length > 0) Some(variableCols.mkString(", ")) else None

    val selectStr = (List(variableColsStr) flatMap (u => u)).mkString(", ")

    val inputQuery = s"""
      SELECT ${selectStr}
      ${ ss.generateSQLBody(r.q) }"""

    val em = ss.headNames // TODO move this dependency analysis to a separate method
    val dependencyRelation = r.q.body map { case(x) => s"${x.name}"}
    var dependencies = List[String]()
    for (e <- em) {
      if (dependencyRelation contains e._2)
        dependencies ::= s""" "extraction_rule_${e._1}" """
    }
    val dependencyStr = if (dependencies.length > 0) s"dependencies: [${dependencies.mkString(", ")}]" else ""

    val extractor = s"""
      deepdive.extraction.extractors.extraction_rule_${index} {
        sql: \"\"\" DROP VIEW IF EXISTS ${r.q.head.name};
        CREATE VIEW ${r.q.head.name} AS ${inputQuery}
        \"\"\"
        style: "sql_extractor"
        ${dependencyStr}
      }
    """
    List(extractor)
  }

  def compile(r: FunctionRule, index: Int, ss: CompilationState): CompiledBlocks = {
    val inputQuery = s"""
    SELECT * FROM ${r.input}
    """

    val function = ss.resolveFunctionName(r.function)

    val dependencies = ss.headNames // TODO move this dependency analysis to a separate method
    // val dependencyRelation = r.q.body map { case(x) => s"${x.name}"}
    var dependency = List[String]()
    for (d <- dependencies) {
      if (r.input == d._2) {
        dependency ::= s""" "extraction_rule_${d._1}" """
      }
    }
    val dependencyStr = if (dependency.length > 0) s"dependencies: [${dependency.mkString(", ")}]" else ""



    val extractor = s"""
      deepdive.extraction.extractors.extraction_rule_${index} {
        input: \"\"\" SELECT * FROM ${r.input}
        \"\"\"
        output_relation: \"${r.output}\"
        udf: \"${function.implementation}\"
        style: \"${function.mode}_extractor\"
        ${dependencyStr}
      }
    """
    List(extractor)
  }

  // generate inference rule part for deepdive
  def compile(r: InferenceRule, i: Int, ss: CompilationState): CompiledBlocks = {
    var blocks = List[String]()
    val qs = new QuerySchema( r.q )

    // node query
    // generate the node portion (V) of the factor graph
    def compileNodeRule(z: InferenceRule, qs: QuerySchema, ss: CompilationState) : CompiledBlocks = {
      val headTerms = z.q.head.terms map {
        case Variable(v,r,i) => s"R${i}.${ss.resolveName(qs.getVar(v)) }"
      }
      val index = qs.getBodyIndex(z.supervision)
      val name  = ss.resolveName(qs.getVar(z.supervision))
      val labelCol = s"R${index}.${name}"
      val headTermsStr = ( "0 as id"  :: headTerms ).mkString(", ")
      val query = s"""SELECT DISTINCT ${ headTermsStr }, ${labelCol} AS label
    ${ ss.generateSQLBody(z.q) }"""

      val dep = ss.headNames
      val dependencyRelation = z.q.body map { case(x) => s"${x.name}"}
      var dependencies = List[String]()
      for (e <- dep) {
        if (dependencyRelation contains e._2)
          dependencies ::= s""" "extraction_rule_${e._1}" """
      }
      val dependencyStr = if (dependencies.length > 0) s"dependencies: [${dependencies.mkString(", ")}]" else ""

      val ext = s"""
      deepdive.extraction.extractors.extraction_rule_${z.q.head.name} {
        sql: \"\"\" DROP TABLE IF EXISTS ${z.q.head.name};
        CREATE TABLE ${z.q.head.name} AS
        ${query}
        \"\"\"
        style: "sql_extractor"
        ${dependencyStr}
      }
    """
      List(ext)
    }
    if (ss.isQueryTerm(r.q.head.name))
      blocks :::= compileNodeRule(r, qs, ss)

    // edge query
    val fakeBody        = r.q.head +: r.q.body
    val fakeCQ          = ConjunctiveQuery(r.q.head, fakeBody) // we will just use the fakeBody below.

    val index = r.q.body.length + 1
    val qs2 = new QuerySchema( fakeCQ )
    val variableIdsStr = Some(s"""R0.id AS "${r.q.head.name}.R0.id" """)
    val variableColsStr = Some(s"""R0.label AS "${r.q.head.name}.R0.label" """)

    // weight string
    val uwStr = r.weights match {
      case KnownFactorWeight(x) => None
      case UnknownFactorWeight(w) => Some(w.flatMap(s => ss.resolveColumn(s, qs2, fakeCQ, true)).mkString(", "))
    }

    val selectStr = (List(variableIdsStr, variableColsStr, uwStr) flatMap (u => u)).mkString(", ")

    // factor input query
    val inputQuery = s"""
      SELECT ${selectStr}
      ${ ss.generateSQLBody(fakeCQ) }"""

    // factor function
    val func = s"""Imply(${r.q.head.name}.R0.label)"""

    // weight
    val weight = r.weights match {
      case KnownFactorWeight(x) => s"${x}"
      case UnknownFactorWeight(w) => {
        s"""?(${w.flatMap(s => ss.resolveColumn(s, qs2, fakeCQ, false)).mkString(", ")})"""
      }
    }

    blocks ::= s"""
      deepdive.inference.factors.factor_${r.q.head.name} {
        input_query: \"\"\"${inputQuery}\"\"\"
        function: "${func}"
        weight: "${weight}"
      }
    """

    blocks.reverse
  }

  def main(args: Array[String]) {
    // get contents of all given files as one flat input program
    val inputProgram = parseArgs(args)
    val parsedProgram = parseProgram(inputProgram)

    // take an initial pass to analyze the parsed program
    val state = new CompilationState( parsedProgram.get )

    // compile the program into blocks of application.conf
    val compiledBlocks = (
      compileUserSettings
      :::
      compileVariableSchema(parsedProgram.get, state)
      :::
      (
      parsedProgram.get.zipWithIndex flatMap {
        // XXX Ideally, a single compile call should handle all the polymorphic
        // cases, but Scala/Java's ad-hoc polymorphism doesn't work that way.
        // Instead, we need to use the visitor pattern, adding compile(...)
        // methods to all case classes of Statement.
        // TODO move state, dependencies args into a composite field of type CompilationState
        // TODO get rid of zipWithIndex by keeping a counter in the CompilationState
        case (s:InferenceRule , i:Int) => compile(s, i, state)
        case (s:ExtractionRule, i:Int) => compile(s, i, state)
        case (s:FunctionRule  , i:Int) => compile(s, i, state)
        case _ => List()
      }
      )
    )

    // emit the generated code
    compiledBlocks foreach println
  }
}
