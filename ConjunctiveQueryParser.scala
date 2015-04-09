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
case class Variable(varName : String, relName: String, index : Int ) 
case class Atom(name : String, terms : List[Variable]) 
case class ConjunctiveQuery(head: Atom, body: List[Atom])

case class WeightedRule(q : ConjunctiveQuery, weights : Option[List[String]]) extends Statement // Weighted rule
case class SchemaElement( a : Atom , query : Boolean ) extends Statement // atom and whether this is a query relation.


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

  def query : Parser[ConjunctiveQuery] = atom ~ ":-" ~ rep1sep(atom, ",") ^^ {
    case (headatom ~ ":-" ~ bodyatoms) => ConjunctiveQuery(headatom, bodyatoms.toList)
  }

  def schema_element : Parser[SchemaElement] = atom ~ opt("!") ^^ {
    case (a ~ None) => SchemaElement(a,true)
    case (a ~ Some(_)) =>  SchemaElement(a,false)
  }


  def rule : Parser[WeightedRule] = query ~ opt( "weight=" ~ rep1sep(col, ",")) ^^ {
    case (q ~ Some("weight=" ~ weights)) => WeightedRule(q,Some(weights))
    case (q ~ None)                      => WeightedRule(q,None)
  }

  // rules or schema elements in aribitrary order
  def statement : Parser[Statement] = (rule | schema_element) ^^ {case(x) => x}

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
      case WeightedRule(_,_) => ()
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
object ConjunctiveQueryParser extends ConjunctiveQueryParser  {

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


  // The input is a weighted rule and our goal is to generate both the
  // node query and the (hyper) edge query. The node query is
  // straightforward using our previous code.

  // The edge query has three parts.

  // The FROM and WHERE clause contain the same terms from node rule, with two extras.
  // (1) We add the head atom into the FROM clause.
  // (2) We add the join conditions to the WHERE clause between the head atom and the body.
  // In the code below, we create a "fake CQ" and generate its body (ignoring the head)

  // The SELECT clause of the query is a bit interesting. 
  // (1) The SELECT clause contains the id of the head relation (if it is a query term)
  // (2) The SELECT clause should also contain the weight attributes (resolved properly)
  // (2) There should be an array_agg( tuple(id1,id2,..) ) of the all query relations in the body.

  // GROUP BY
  // We should have a group that contains the head variable and the weight attributes.
  def weightedRule( ss: StatementSchema, r : WeightedRule ) : Tuple2[Option[String], Option[String] ] = {
    val node_query = if (ss.isQueryTerm(r.q.head.name)) Some(nodeRule(ss,r.q)) else None  
    val edge_query = {
      // in the code below, we rely on the head being the last atom for indexing (since we index R{index})
      val fakeBody        = r.q.body :+ r.q.head
      val fakeCQ          = ConjunctiveQuery(r.q.head, fakeBody) // we will just use the fakeBody below.

      // Generate the body of the query.
      val qs              = new QuerySchema( r.q )
      val body_attributes = r.q.body.zipWithIndex flatMap {
        // check if relName is a ground term, if so skip it.
        // if not, generate the id column.
        case (Atom(r,_),i) =>
          if(ss.isQueryTerm(r)) Some(s"R${i}._id") else None
      } // we know have all variables in the body

      // Construct the various terms for the select and group by
      val factor_id_select = Some("0 as _fid")
      val factor_id        = Some("_fid")
      val head_id          = if (ss.isQueryTerm(r.q.head.name)) Some(s"R${ r.q.body.length }._id") else None

      // does array agg need a tuple constructor?
      val array_agg        = if (body_attributes.length > 0) Some(s"array_agg(${ body_attributes.mkString(", ") })") else None

      val uw_str =
        r.weights match {
          case None => None
          case Some(w) =>
            val uw = w map {
              case(s) =>
                s"R${ qs.getBodyIndex(s) }.${ ss.resolveName( qs.getVar(s) ) }"
            }
            Some(s"${ uw.mkString(", ") }")
        }

      val select_str = (List(factor_id_select, head_id, array_agg, uw_str) flatMap { case(u) => u }).mkString(", ")
      val group_str  = (List(factor_id, head_id, uw_str) flatMap { case(u) => (u) }).mkString(", ")

      val u = s"""
        SELECT ${select_str} 
        ${ generateSQLBody(ss, fakeCQ) } 
        GROUP BY ${group_str} """
      // if no random variables in the query then don't emit a factor term
      if (ss.isQueryTerm(r.q.head.name) || body_attributes.length > 0) Some(u) else None
    }
    (node_query, edge_query)
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
  def main(args: Array[String]) = {
    val q      = parse(statements, "S(a1,a2); R(pk,f); Q(x) :- R(x,f) weight=f; Q(x) :- S(x,y),T(y); T(base_attr)!; R(x,y) :- U(x,y); S(x,y) :- R(x,y);")
    val schema = new StatementSchema( q.get )

    val queries = q.get flatMap {
      case _ : SchemaElement => None
      case w : WeightedRule =>
        Some(weightedRule(schema,w))
    }
    queries.foreach { case(query) => println(query) }
  }
}
