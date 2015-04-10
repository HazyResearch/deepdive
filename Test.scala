import scala.util.parsing.combinator._
import scala.collection.immutable.HashMap

trait Statement
case class Variable(varName : String, relName: String, index : Int ) 
case class Atom(name : String, terms : List[Variable]) 
case class ConjunctiveQuery(head: Atom, body: List[Atom])

case class SchemaElement( a : Atom , query : Boolean ) extends Statement // atom and whether this is a query relation.
case class ExtractionRule(q : ConjunctiveQuery, udfs : Option[String]) extends Statement // Extraction rule
case class InferenceRule(q : ConjunctiveQuery, weights : Option[List[String]]) extends Statement // Inference rule


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

  def schema_element : Parser[SchemaElement] = atom ~ opt("!") ^^ {
    case (a ~ None) => SchemaElement(a,true)
    case (a ~ Some(_)) =>  SchemaElement(a,false)
  }

  def extraction_rule : Parser[ExtractionRule] = query ~ opt( "udf=" ~ udf) ^^ {
    case (q ~ Some("udf=" ~ udfs)) => ExtractionRule(q,Some(udfs))
    case (q ~ None)                => ExtractionRule(q,None)
  }

  def inference_rule : Parser[InferenceRule] = query ~ opt( "weight=" ~ rep1sep(col, ",")) ^^ {
    case (q ~ Some("weight=" ~ weights)) => InferenceRule(q,Some(weights))
    case (q ~ None)                      => InferenceRule(q,None)
  }


  // rules or schema elements in aribitrary order
  def statement : Parser[Statement] = (extraction_rule | inference_rule | schema_element) ^^ {case(x) => x}

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
      case InferenceRule(_,_) => ()
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
    val bodyNames = ( z.body.zipWithIndex map { case(x,i) => s"${x.name} as R${i}"}).mkString(",")
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

    s"""FROM ${ bodyNames } ${ whereClauseStr }"""
  }
  // generate the node portion (V) of the factor graph
  def nodeRule(ss : StatementSchema, z : ConjunctiveQuery) : String = {
    val headTerms = z.head.terms map {
      case Variable(v,r,i) => s"R${i}.${ss.resolveName(Variable(v,r,i)) }"
    }
    val headTermsStr = ( "0 as _id"  :: headTerms ).mkString(",")
    s"""CREATE TABLE ${ z.head.name } AS
    SELECT DISTINCT ${ headTermsStr }
    ${ generateSQLBody(ss,z) }
     """
  }

  // Generate extraction rule part for deepdive
  def extractionRule( ss: StatementSchema, r : ExtractionRule ) : String = {
    println(r.udfs.get)
    // Generate the body of the query.
    val qs              = new QuerySchema( r.q )
    // variable columns
    val variableCols = r.q.head.terms flatMap {
      case(Variable(v,rr,i)) => {
        val index = qs.getBodyIndex(v)
        val name  = ss.resolveName(qs.getVar(v))
        val relation = r.q.body(index).name
        Some(s"""R${index}.${name} AS "${relation}.R${index}.${name}" """)
      }
    }

    val variableColsStr = if (variableCols.length > 0) Some(variableCols.mkString(", ")) else None
    
    val selectStr = (List(variableColsStr) flatMap (u => u)).mkString(", ")
    
    println(s"${selectStr}")
    val inputQuery = s"""
      SELECT ${selectStr} 
      ${ generateSQLBody(ss, r.q) }"""

    
    val extractor = s"""
      e_${r.udfs.get} {
        input : \"\"\" ${inputQuery}
        \"\"\"
        output_relation : \"${r.q.head.name}\"
        udf : \"/udf/${r.udfs.get}.py\"
        style : \"tsv_extractor\"
      }
    """
    extractor
  }

  def main(args: Array[String]) = {
    var input = """
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
    // val q = parse(statements, args(0))
    val q = parse(statements, input)
    val schema = new StatementSchema( q.get )
    val queries = q.get flatMap {
      case _ : SchemaElement => None
      case e : ExtractionRule =>
        Some(extractionRule(schema, e))
      // case w : InferenceRule =>
      //   Some(inferenceRule(schema,w))
    }
    for (query <- queries)
      println(query)
  }
}
