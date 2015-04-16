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
case class ConjunctiveQuery(head: Atom, body: List[Atom])

sealed trait FactorWeight {
  def variables : List[String]
}
case class KnownFactorWeight(value: Double) extends FactorWeight {
  def variables = Nil
}
case class UnknownFactorWeight(variables: List[String]) extends FactorWeight

case class SchemaElement( a : Atom , query : Boolean ) extends Statement // atom and whether this is a query relation.
case class ExtractionRule(q : ConjunctiveQuery, udfs : Option[String]) extends Statement // Extraction rule
case class InferenceRule(q : ConjunctiveQuery, weights : FactorWeight, supervision : String) extends Statement // Weighted rule


// Parser
class ConjunctiveQueryParser extends JavaTokenParsers {   
  // Odd definitions, but we'll keep them.
  def stringliteral1: Parser[String] = ("'"+"""([^'\p{Cntrl}\\]|\\[\\"'bfnrt]|\\u[a-fA-F0-9]{4})*"""+"'").r ^^ {case (x) => x}
  def stringliteral2: Parser[String] = """[a-zA-Z_0-9\.]*""".r ^^ {case (x) => x}
  def stringliteral: Parser[String] = (stringliteral1 | stringliteral2) ^^ {case (x) => x}

  // relation names and columns are just strings.
  def relation_name: Parser[String] = stringliteral ^^ {case (x) => x}
  def col : Parser[String] = stringliteral  ^^ { case(x) => x }

  def atom: Parser[Atom] = relation_name ~ "(" ~ rep1sep(col, ",") ~ ")" ^^ {
    case (r ~ "(" ~ cols ~ ")") => {
      val vars = cols.zipWithIndex map { case(name,i) => Variable(name, r, i) }
      Atom(r,vars)
    }
  }

  def udf : Parser[String] = stringliteral ^^ {case (x) => x}

  def query : Parser[ConjunctiveQuery] = atom ~ ":-" ~ rep1sep(atom, ",") ^^ {
    case (headatom ~ ":-" ~ bodyatoms) => ConjunctiveQuery(headatom, bodyatoms.toList)
  }

  def schemaElement : Parser[SchemaElement] = atom ~ opt("!") ^^ {
    case (a ~ None) => SchemaElement(a,true)
    case (a ~ Some(_)) =>  SchemaElement(a,false)
  }

  def extractionRule : Parser[ExtractionRule] = query ~  "udf" ~ "=" ~ opt(udf) ^^ {
    case (q ~ "udf" ~ "=" ~ Some(udfs)) => ExtractionRule(q,Some(udfs))
    case (q ~ "udf" ~ "=" ~ None)       => ExtractionRule(q,None)
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
  def statement : Parser[Statement] = (extractionRule | inferenceRule | schemaElement) ^^ {case(x) => x}

  def statements : Parser[List[Statement]] = rep1sep(statement, ";") ^^ { case(x) => x }
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

  def init() = {    
    // generate the statements.
    statements.foreach {
      case SchemaElement(Atom(r, terms),query) =>
        terms.foreach {
          case Variable(n,r,i) =>
            schema           += { (r,i) -> n }
            ground_relations += { r -> query } // record whether a query or a ground term.
        }
      case ExtractionRule(_,_) => ()
      case InferenceRule(_,_,_) => ()
    }
    println(schema)
    println(ground_relations)
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
object Test extends ConjunctiveQueryParser  {

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
  def nodeRule(ss : StatementSchema, qs: QuerySchema, z : InferenceRule) : String = {
    val headTerms = z.q.head.terms map {
      case Variable(v,r,i) => s"R${i}.${ss.resolveName(qs.getVar(v)) }"
    }
    val index = qs.getBodyIndex(z.supervision)
    val name  = ss.resolveName(qs.getVar(z.supervision))
    val labelCol = s"R${index}.${name}"
    val headTermsStr = ( "0 as id"  :: headTerms ).mkString(", ")
    s"""CREATE TABLE ${ z.q.head.name } AS
    SELECT DISTINCT ${ headTermsStr }, ${labelCol} AS label
    ${ generateSQLBody(ss,z.q) }
     """
  }

  // generate variable schema statements
  def variableSchema(statements : List[Statement], ss: StatementSchema) : String = {
    var schema = Set[String]() 
    // generate the statements.
    statements.foreach {
      case InferenceRule(q, weights, supervision) =>
        val qs = new QuerySchema(q)
        q.head.terms.foreach {
          case Variable(n,r,i) => {
            println(n)
            val index = qs.getBodyIndex(n)
            val name  = ss.resolveName(qs.getVar(n))
            val relation = q.body(index).name
            schema += s"${relation}.${name} : Boolean"
          }
        }
      case _ => ()
    }
    val ddSchema = schema.mkString("\n")
    println(ddSchema)
    ddSchema
  }

  // Generate extraction rule part for deepdive
  def extractionRule( ss: StatementSchema, em: List[(Int, String)], r : ExtractionRule, index : Int ) : String = {
    // Generate the body of the query.
    val qs              = new QuerySchema( r.q )
    // variable columns
    val variableCols = r.q.head.terms flatMap {
      case(Variable(v,rr,i)) => resolveColumn(v, ss, qs, r.q, true)
    }

    val variableColsStr = if (variableCols.length > 0) Some(variableCols.mkString(", ")) else None
    
    val selectStr = (List(variableColsStr) flatMap (u => u)).mkString(", ")
    
    // println(s"${selectStr}")
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
      extraction_rule_${index} {
        input : \"\"\" ${inputQuery}
        \"\"\"
        output_relation : \"${r.q.head.name}\"
        udf : \"/udf/${r.udfs.get}.py\"
        style : \"tsv_extractor\"
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
  def inferenceRule(ss : StatementSchema, r : InferenceRule) : String = {
    println("==================")
    val qs = new QuerySchema( r.q )

    // node query
    val node_query = if (ss.isQueryTerm(r.q.head.name)) Some(nodeRule(ss,qs,r)) else None  
    println(node_query)

    // edge query
    val fakeBody        = r.q.head +: r.q.body 
    println(fakeBody)
    val fakeCQ          = ConjunctiveQuery(r.q.head, fakeBody) // we will just use the fakeBody below.

    // Generate the body of the query.
    // check if relName is a ground term, if so skip it.
    // if not, generate the id column.
    // val variableIds = r.q.body.zipWithIndex flatMap {
    //   case (Atom(r,_),i) =>
    //     if(ss.isQueryTerm(r)) Some(s"""R${i}.id AS "${r}.R${i}.id" """) else None
    // } // we know have all variables in the body

    // val variableIdsStr = if (variableIds.length > 0) Some(variableIds.mkString(", ")) else None

    // variable columns
    // val variableCols = r.q.head.terms flatMap {
    //   case(Variable(v,rr,i)) => resolveColumn(v, ss, qs, r.q, true)
    // }
    // val variableColsStr = if (variableCols.length > 0) Some(variableCols.mkString(", ")) else None
    
    val variableIdsStr = Some(s"""R0.id AS "${r.q.head.name}.R0.id" """)
    val variableColsStr = Some(s"""R0.label AS "${r.q.head.name}.R0.label" """)

    // weight string
    val uwStr = r.weights match {
      case KnownFactorWeight(x) => None
      case UnknownFactorWeight(w) => Some(w.flatMap(s => resolveColumn(s, ss, qs, r.q, true)).mkString(", "))
    }

    val selectStr = (List(variableIdsStr, variableColsStr, uwStr) flatMap (u => u)).mkString(", ")

    // factor input query
    val inputQuery = s"""
      SELECT ${selectStr} 
      ${ generateSQLBody(ss, fakeCQ) }"""

    // variable columns using alias (for factor function)
    // val variableColsAlias = r.q.head.terms flatMap {
    //   case(Variable(v,rr,i)) => resolveColumn(v, ss, qs, r.q, false)
    // }
    // val variableColsAliasStr = if (variableColsAlias.length > 0) Some(variableColsAlias.mkString(", ")) else None

    // factor function
    val func = s"""Imply(${r.q.head.name}.R0.label)"""

    // weight
    val weight = r.weights match {
      case KnownFactorWeight(x) => s"${x}"
      case UnknownFactorWeight(w) => {
        s"""?(${w.flatMap(s => resolveColumn(s, ss, qs, r.q, false)).mkString(", ")})"""
      }
    }
    
    val rule = s"""
      factor_${r.q.head.name} {
        input_query: \"\"\"${inputQuery}\"\"\"
        function: "${func}"
        weight: "${weight}"
      }
    """
    println(rule)

    return inputQuery
  }
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
    val q      = parse(statements, test3)
    val schema = new StatementSchema( q.get )
    val variables = variableSchema(q.get, schema)

    val extracions = q.get flatMap {
      case _ : SchemaElement  => None
      case e : ExtractionRule => Some(e)
      case w : InferenceRule  => None
    }
    val extractionsWithIndex = extracions.zipWithIndex
    val extractionMap = extractionsWithIndex map {
      case (e) => (e._2, e._1.q.head.name)
    }
    // for (extractor <- extractionsWithIndex) {
    //   extractionMap += (extractor.get(1), extractor.get(0).q.head)
    // }

    println(extractionMap)

    val queries = extractionsWithIndex flatMap {
      case (e) => Some(extractionRule(schema, extractionMap, e._1, e._2))
    }

    q.get flatMap {
      case w : InferenceRule  => Some(inferenceRule(schema, w))
      case _ => None
    }

    // println(extractionsWithIndex)
    // println(extracions)
    // val queries = q.get flatMap {
    //   case _ : SchemaElement  => None
    //   case e : ExtractionRule => Some(extractionRule(schema, extractionMap, e))
  }
}
