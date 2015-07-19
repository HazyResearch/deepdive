package org.deepdive.ddlog

import scala.collection.immutable.HashMap
import scala.collection.immutable.HashSet

// semantic checker for ddlog
object DeepDiveLogSemanticChecker extends DeepDiveLogHandler {

  // initialize the checker
  def init(program: DeepDiveLog.Program) {
    program foreach {
      case s: SchemaDeclaration => {
        heads += s.a.name
        schemaDeclaration += { s.a.name -> s }
      }
      case ExtractionRule(q, supervision) => {
        heads += q.head.name
      }
      case InferenceRule(q, weight, semantic, mode) => {
        heads += q.head.name
      }
      case f: FunctionDeclaration => {
        functionDeclaration += { f.functionName -> f }
      }
      case FunctionCallRule(input, output, function) => {
        heads += output
      }
    }
  }

  // check a statement
  def check(stmt: Statement) {
    checkRelationDefined(stmt)
    checkFunctionDefined(stmt)
    checkVariableRelationSchema(stmt)
    checkNumberOfColumns(stmt)
  }

  // check if relations in the body are defined
  def checkRelationDefined(stmt: Statement) {
    val stmtStr = DeepDiveLogPrettyPrinter.print(stmt)
    def checkRelation(name: String) {
      if (!(heads contains name))
        error(stmt, s"""relation "${name}" is not defined""")
    }
    stmt match {
      case s: ExtractionRule => {
        s.q.bodies foreach { x =>
          x foreach {
            case a: Atom => checkRelation(a.name)
            case _ =>
          }
        }
      }
      case s: InferenceRule => {
        s.q.bodies foreach { x =>
          x foreach {
            case a: Atom => checkRelation(a.name)
            case _ =>
          }
        }
      }
      case s: FunctionCallRule => checkRelation(s.input)
      case _ =>
    }
  }

  // check if a function is defined when it's called
  def checkFunctionDefined(stmt: Statement) {
    stmt match {
      case s: FunctionCallRule => {
        if (!(functionDeclaration.keySet contains s.function))
          error(stmt, s"""function "${s.function}" is not defined""")
      }
      case _ =>
    }
  }

  // check if the user use reserved column names
  def checkVariableRelationSchema(stmt: Statement) {
    val reservedSet = Set("id", "label")
    stmt match {
      case SchemaDeclaration(Attribute(r, terms, types), isQuery, vType) => {
        if (isQuery) {
          terms.foreach { case name =>
            if (reservedSet contains name)
              error(stmt, s"""variable relation contains reserved column "${name}" """)
          }
        }
      }
      case _ =>
    }
  }

  // check if the number of columns match schema declaration
  def checkNumberOfColumns(stmt: Statement) {
    def checkAtom(a: Atom) {
      if ((schemaDeclaration.keySet contains a.name) &&
        (a.terms.size != schemaDeclaration(a.name).a.terms.size))
        error(stmt, s""""${a.name}": number of columns in the query does not match number of columns in the schema""")
    }
    def checkCq(cq: ConjunctiveQuery) {
      checkAtom(cq.head)
      cq.bodies foreach { x =>
        x foreach {
          case a: Atom => checkAtom(a)
          case _ =>
        }
      }
    }
    stmt match {
      case s: ExtractionRule => checkCq(s.q)
      case s: InferenceRule  => checkCq(s.q)
      case _ =>
    }
  }

  // throw exception
  def error(stmt: Statement, message: String) {
    val stmtStr = DeepDiveLogPrettyPrinter.print(stmt)
    throw new RuntimeException(message + s"\n${stmtStr}")
  }

  // run the checker
  override def run(program: DeepDiveLog.Program, config: DeepDiveLog.Config) = {
    init(program)
    program foreach check
  }

  // schema declaration
  var heads  : Set[String]                       = new HashSet[String]()
  // schema
  var schemaDeclaration : Map[String, SchemaDeclaration] = new HashMap[String, SchemaDeclaration]()
  // function declaration
  var functionDeclaration : Map[String, FunctionDeclaration] = new HashMap[String, FunctionDeclaration]()

}