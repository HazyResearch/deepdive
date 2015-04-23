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

  def statements : Parser[List[Statement]] = rep1sep(statement, ".") ^^ { case(x) => x }
}

// This handles the schema statements.
// It can tell you if a predicate is a "query" predicate or a "ground prediate"
// and it resolves Variables their correct and true name in the schema, i.e. R(x,y) then x could be Attribute1 declared.
class StatementSchema( statements : List[Statement] )  {
    // TODO: refactor the schema into a class that constructs and
    // manages these maps. Also it should have appropriate
    // abstractions and error handling for missing values.
    // ** Start refactor.
  var schema : Map[ Tuple2[String,Int], String ] = new HashMap[ Tuple2[String,Int], String ]()

  var ground_relations : Map[ String, Boolean ]  = new HashMap[ String, Boolean ]()

  var function_schema : Map[String, FunctionElement] = new HashMap[ String, FunctionElement]()

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
    // println(schema)
    // println(ground_relations)
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


object ddlc extends ConjunctiveQueryParser  {

  // This is generic code that generates the FROM with positional aliasing R0, R1, etc.
  // and the corresponding WHERE clause (equating all variables)
  def generateSQLBody(ss : StatementSchema, z : ConjunctiveQuery) : String = {
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
              val real_attr_name1 = ss.resolveName( Variable(varName, relName, index) )
              val real_attr_name2 = ss.resolveName( qs.getVar(varName))
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
  // generate the node portion (V) of the factor graph
  def nodeRule(ss : StatementSchema, qs: QuerySchema, z : InferenceRule, dep: List[(Int, String)]) : String = {
    val headTerms = z.q.head.terms map {
      case Variable(v,r,i) => s"R${i}.${ss.resolveName(qs.getVar(v)) }"
    }
    val index = qs.getBodyIndex(z.supervision)
    val name  = ss.resolveName(qs.getVar(z.supervision))
    val labelCol = s"R${index}.${name}"
    val headTermsStr = ( "0 as id"  :: headTerms ).mkString(", ")
    val query = s"""SELECT DISTINCT ${ headTermsStr }, ${labelCol} AS label
    ${ generateSQLBody(ss,z.q) }"""
    
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
    println(ext)
    ext
  }

  // generate variable schema statements
  def variableSchema(statements : List[Statement], ss: StatementSchema) : String = {
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
    println(ddSchema)
    ddSchema
  }

  // Generate extraction rule part for deepdive
  def extractionRule( ss: StatementSchema, em: List[(Int, String)], r : ExtractionRule, index : Int) : String = {
    // Generate the body of the query.
    val qs              = new QuerySchema( r.q )
    // variable columns
    val variableCols = r.q.head.terms flatMap {
      case(Variable(v,rr,i)) => resolveColumn(v, ss, qs, r.q, true)
    }

    val variableColsStr = if (variableCols.length > 0) Some(variableCols.mkString(", ")) else None
    
    val selectStr = (List(variableColsStr) flatMap (u => u)).mkString(", ")
    
    val inputQuery = s"""
      SELECT ${selectStr} 
      ${ generateSQLBody(ss, r.q) }"""

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
    println(extractor)
    extractor
  }

  def functionRule( ss: StatementSchema, dependencies: List[(Int, String)], r : FunctionRule, index : Int) : String = {
    
    val inputQuery = s"""
    SELECT * FROM ${r.input}
    """

    val function = ss.resolveFunctionName(r.function)    

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
    println(extractor)
    extractor
  }


  // resolve a column name with alias
  def resolveColumn(s: String, ss: StatementSchema, qs: QuerySchema, q : ConjunctiveQuery,
   alias: Boolean) : Option[String] = {
    val index = qs.getBodyIndex(s)
    val name  = ss.resolveName(qs.getVar(s))
    val relation = q.body(index).name
    if (alias)
      Some(s"""R${index}.${name} AS "${relation}.R${index}.${name}" """)
    else
      Some(s"${relation}.R${index}.${name}")
  }

  // generate inference rule part for deepdive
  def inferenceRule(ss : StatementSchema, r : InferenceRule, dep : List[(Int, String)]) : String = {
    val qs = new QuerySchema( r.q )

    // node query
    val node_query = if (ss.isQueryTerm(r.q.head.name)) Some(nodeRule(ss,qs,r, dep)) else None  

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
      case UnknownFactorWeight(w) => Some(w.flatMap(s => resolveColumn(s, ss, qs2, fakeCQ, true)).mkString(", "))
    }

    val selectStr = (List(variableIdsStr, variableColsStr, uwStr) flatMap (u => u)).mkString(", ")

    // factor input query
    val inputQuery = s"""
      SELECT ${selectStr} 
      ${ generateSQLBody(ss, fakeCQ) }"""

    // factor function
    val func = s"""Imply(${r.q.head.name}.R0.label)"""

    // weight
    val weight = r.weights match {
      case KnownFactorWeight(x) => s"${x}"
      case UnknownFactorWeight(w) => {
        s"""?(${w.flatMap(s => resolveColumn(s, ss, qs2, fakeCQ, false)).mkString(", ")})"""
      }
    }
    
    val rule = s"""
      deepdive.inference.factors.factor_${r.q.head.name} {
        input_query: \"\"\"${inputQuery}\"\"\"
        function: "${func}"
        weight: "${weight}"
      }
    """
    println(rule)

    return inputQuery
  }

  def dbSettings() : String = """
  deepdive.db.default {
    driver: "org.postgresql.Driver"
    url: "jdbc:postgresql://"${PGHOST}":"${PGPORT}"/"${DBNAME}
    user: ${PGUSER}
    password: ${PGPASSWORD}
    dbname: ${DBNAME}
    host: ${PGHOST}
    port: ${PGPORT}
  }
  """
  /*
   T(base_attr);
   S(a1,a2)
   Q(x) :- S(x,y),T(y) 
   Should generate.

   Node query:
   CREATE TABLE Q AS
   SELECT 0 as _id, R0.a1
   FROM S as R0,T as R1 
   WHERE R0.a2 = R1.base_attr
   
   Edge Query (if S and T are probabilistic)
   SELECT Q._id, array_agg( (S._id, T_.id) )
   FROM Q as R0,S as R1,T as R2 
   WHERE S.y = T.base_attr AND 
         Q.x = S.x AND Q.z = S.z  
   
   Factor Function: OR

   =======
   R(x,y) (assume non probabilistic)

   Q(x) :- R(x,f) weight=f

   Node Query:
   CREATE TABLE Q AS
   SELECT DISTINCT 0 as _id, x FROM R
   
   Edge Query:
   SELECT 0 as _fid, Q.id, R.f as w
   FROM Q, R
   WHERE Q.x = R.x

   =======
   
   */
  def main(args: Array[String]) {
    val test1 = """
      S(a1,a2); 
      R(pk,f)!; 
      Q(x) :- R(x,f) weight=f;
      Q2(x) :- R(x, f), S(x, y) weight = f"""
    val test2 = """
      S(a1,a2); 
      R(pk,f); 
      Q(x) :- R(x,f) weight=f; 
      Q(x) :- S(x,y),T(y); 
      T(base_attr)!; 
      R(y,x) :- U(x,y); 
      S(x,y) :- R(x,y);"""
    val test3 = """
      has_spouse(person1_id, person2_id, sentence_id, description, is_true, relation_id);
      has_spouse_features(relation_id, feature);
      q(rid)!;

      q(y) :-
        has_spouse(a, b, c, d, x, y),
        has_spouse_features(y, f)
        weight = f
        label = x;
      q(y) :-
        has_spouse(a, b, c, d, x, y),
        has_spouse_features(y, f)
        weight = f
        label = x;
        """

    //   f_has_spouse_symmetry(x, y) :-
    //     has_spouse(a1, a2, a3, a4, x, a6),
    //     has_spouse(a2, a1, b3, b4, y, b6)
    //     weight = 1;
    // """
    val test4 = """
      articles(article_id, text);
      sentences(document_id, sentence, words, lemma, pos_tags, dependencies, ner_tags, sentence_offset, sentence_id);
      people_mentions(sentence_id, start_position, length, text, mention_id);
      has_spouse(person1_id, person2_id, sentence_id, description, is_true, relation_id, id);
      has_spouse_features(relation_id, feature);
      people_mentions(sentence_id, words, ner_tags):- 
        sentences(document_id, sentence, words, lemma, pos_tags, dependencies, ner_tags, sentence_offset, sentence_id)
      udf=ext_people;
      has_spouse(sentence_id, p1.mention_id, p1.text, p2.mention_id, p2.text):-
        people_mentions(sentence_id, p1.start_position, p1.length, p1.text, p1.mention_id),
        people_mentions(sentence_id, p2.start_position, p2.length, p2.text, p2.mention_id)
      udf=ext_has_spouse;
      has_spouse_features(words, relation_id, p1.start_position, p1.length, p2.start_position, p2.length):-
        sentences(s.document_id, s.sentence, words, s.lemma, s.pos_tags, s.dependencies, s.ner_tags, s.sentence_offset, sentence_id),
        has_spouse(person1_id, person2_id, sentence_id, h.description, h.is_true, relation_id, h.id),
        people_mentions(sentence_id, p1.start_position, p1.length, p1.text, person1_id),
        people_mentions(sentence_id, p2.start_position, p2.length, p2.text, person2_id)
      udf=ext_has_spouse_features;
    """

    val test5 = """
      ext_people_input(
        sentence_id,
        words,
        ner_tags).
      function ext_has_spouse_features over like ext_has_spouse_features_input
                                  returns like has_spouse_features
      implementation udf/ext_has_spouse_features.py handles tsv lines.
      function ext_people over like ext_people_input
                     returns like people_mentions
      implementation udf/ext_people.py handles tsv lines.
      ext_people_input(sentence_id, words, ner_tags):- 
        sentences(document_id, sentence, words, lemma, pos_tags, dependencies, ner_tags, sentence_offset, sentence_id).
      people_mentions :-
      !ext_people(ext_people_input).
      people_mentions_1 :-
      !ext_people(people_mentions).
    """

    val test6 = """
    articles(
      article_id text,
      text       text).
    sentences(
      document_id     text,
      sentence        text,
      words           text[],
      lemma           text[],
      pos_tags        text[],
      dependencies    text[],
      ner_tags        text[],
      sentence_offset int,
      sentence_id     text).
    people_mentions(
      sentence_id    text,
      start_position int,
      length         int,
      text           text,
      mention_id     text).

    has_spouse_candidates(
      person1_id  text,
      person2_id  text,
      sentence_id text,
      description text,
      relation_id text).
    has_spouse_features(
      relation_id text,
      feature     text).

    has_spouse(relation_id text)?.
    
    people_mentions :-
      !ext_people(ext_people_input).

    ext_people_input(
      sentence_id text,
      words       text[],
      ner_tags    text[]).

    ext_people_input(s, words, ner_tags) :-
      sentences(a, b, words, c, d, e, ner_tags, f, s).

    function ext_people over like ext_people_input
                     returns like people_mentions
      implementation "/Users/feiran/workspace/release/deepdive/app/spouse_datalog/udf/ext_people.py" handles tsv lines.

    has_spouse_candidates :-
      !ext_has_spouse(ext_has_spouse_input).

    ext_has_spouse_input(
      sentence_id text,
      p1_id       text,
      p1_text     text,
      p2_id       text,
      p2_text     text).

    ext_has_spouse_input(s, p1_id, p1_text, p2_id, p2_text) :-
      people_mentions(s, a, b, p1_text, p1_id),
      people_mentions(s, c, d, p2_text, p2_id).

    function ext_has_spouse over like ext_has_spouse_input
                         returns like has_spouse_candidates
      implementation "/Users/feiran/workspace/release/deepdive/app/spouse_datalog/udf/ext_has_spouse.py" handles tsv lines.

    has_spouse_features :-
      !ext_has_spouse_features(ext_has_spouse_features_input).

    ext_has_spouse_features_input(
      words             text[],
      relation_id       text,
      p1_start_position int,
      p1_length         int,
      p2_start_position int,
      p2_length         int).

    ext_has_spouse_features_input(words, rid, p1idx, p1len, p2idx, p2len) :-
      sentences(a, b, words, c, d, e, f, g, s),
      has_spouse_candidates(person1_id, person2_id, s, h, rid, x),
      people_mentions(s, p1idx, p1len, k, person1_id),
      people_mentions(s, p2idx, p2len, l, person2_id).
      
    function ext_has_spouse_features over like ext_has_spouse_features_input
                                  returns like has_spouse_features
      implementation "/Users/feiran/workspace/release/deepdive/app/spouse_datalog/udf/ext_has_spouse_features.py" handles tsv lines.

    has_spouse(rid) :-
      has_spouse_candidates(a, b, c, d, rid, l),
      has_spouse_features(rid, f)
    weight = f
    label = l.
    """
    println(dbSettings())
    val q      = parse(statements, test6)
    val schema = new StatementSchema( q.get )
    val variables = variableSchema(q.get, schema)
    var dependencies = q.get.zipWithIndex map {
      case (e : ExtractionRule, i) => (i, e.q.head.name)
      case (f : FunctionRule, i) => (i, f.output)
      case (w : InferenceRule, i) => (i, w.q.head.name)
      case (_,_) => (-1, "-1")
    }
    val queries = q.get.zipWithIndex flatMap {
      case (e : ExtractionRule, i) => Some(extractionRule(schema, dependencies, e, i))
      case (w : InferenceRule, i)  => Some(inferenceRule(schema, w, dependencies))
      case (f : FunctionRule, i) => Some(functionRule(schema, dependencies, f, i))
      case (_,_) => None
    }
  }
}
