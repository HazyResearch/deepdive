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
      case s: ExtractionRule => {
        heads += s.headName
      }
      case s: InferenceRule => {
        heads += s.headName
      }
      case s: FunctionDeclaration => {
        functionDeclaration += { s.functionName -> s }
      }
      case s: FunctionCallRule => {
        heads += s.output
      }
    }
  }

  // check a statement
  def check(stmt: Statement) {
    checkRelationDefined(stmt)
    checkFunctionDefined(stmt)
    checkVariableRelationSchema(stmt)
    checkNumberOfColumns(stmt)
    checkQuantifiedBody(stmt)
    checkWeight(stmt)
    checkVariableBindings(stmt)
  }

  // iterate over all atoms contained in the body list and apply the checker
  def checkBodyAtoms(checker: BodyAtom => Unit): List[Body] => Unit = bodies => {
    bodies foreach {
      case a: BodyAtom => checker(a)
      case a: QuantifiedBody => checkBodyAtoms(checker)(a.bodies)
      case _ =>
    }
  }

  // check if relations in the body are defined
  def checkRelationDefined(stmt: Statement) {
    val stmtStr = DeepDiveLogPrettyPrinter.print(stmt)
    def checkRelation(a: BodyAtom) {
      if (!(heads contains a.name))
        error(stmt, s"""relation "${a.name}" is not defined""")
    }
    def check = checkBodyAtoms(checkRelation)
    stmt match {
      case s: ExtractionRule => s.q.bodies foreach check
      case s: InferenceRule => s.q.bodies foreach check
      case s: FunctionCallRule => s.q.bodies foreach check
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
      case decl: SchemaDeclaration => {
        if (decl.isQuery) {
          decl.a.terms.foreach { case name =>
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
    def check(name: String, size: Int) {
      if ((schemaDeclaration.keySet contains name) &&
        (size != schemaDeclaration(name).a.terms.size))
        error(stmt, s""""${name}": number of columns in the query does not match number of columns in the schema""")
    }
    def checkBodyAtom(a: BodyAtom) = check(a.name, a.terms.size)
    def checkCq(headName: String, cq: ConjunctiveQuery) {
      check(headName, cq.headTerms.size)
      cq.bodies foreach checkBodyAtoms(checkBodyAtom)
    }
    stmt match {
      case s: ExtractionRule   => checkCq(s.headName, s.q)
      case s: InferenceRule    => checkCq(s.headName, s.q)
      case _ =>
    }
  }

  // check if quantified body contains required number of atoms
  def checkQuantifiedBody(stmt: Statement) {
    def checkBody(bodies: List[Body]) {
      bodies.foreach {
        case b: QuantifiedBody => {
          b.modifier match {
            case OuterModifier() =>
              if ((b.bodies collect { case x: BodyAtom => 1 }).size != 1)
                error(stmt, s"One and only one atom should be supplied in OPTIONAL modifier")
            case ExistModifier(_) =>
              if ((b.bodies collect { case x: BodyAtom => 1 }).size == 0)
                error(stmt, s"At least one atom should be supplied in EXISTS modifier")
            case AllModifier() =>
              if ((b.bodies collect { case x: BodyAtom => 1 }).size == 0)
                error(stmt, s"At least one atom should be supplied in ALL modifier")
          }
        }
        case _ =>
      }
    }
    stmt match {
      case s: ExtractionRule => s.q.bodies foreach checkBody
      case s: InferenceRule => s.q.bodies foreach checkBody
      case _ =>
    }
  }

  // check if the weights makes sense
  def checkWeight(stmt: Statement) {
    stmt match {
      case s: InferenceRule => {
        if ((s.weights.variables collect { case x: ConstExpr => x }).size >= 2)
          error(stmt, s"Weight variables can contain at most one constant")
      }
      case _ =>
    }
  }

  // collect variables used in the expression
  def collectUsedVars(expr: Expr) : Set[String] = expr match {
    case VarExpr(name) => Set(name)
    case FuncExpr(function, args, agg) => args flatMap collectUsedVars toSet
    case BinaryOpExpr(lhs, op, rhs) => collectUsedVars(lhs) ++ collectUsedVars(rhs)
    case TypecastExpr(lhs, rhs) => collectUsedVars(lhs)
    case _ => Set()
  }

  def collectUsedVars(pattern: Pattern) : Set[String] = pattern match {
    case VarPattern(name) => Set(name)
    case ExprPattern(e) => collectUsedVars(e)
    case PlaceholderPattern() => Set()
  }

  def collectDefinedVars(pattern: Pattern) : Set[String] = pattern match {
    case VarPattern(name) => Set(name)
    case _ => Set()
  }

  def collectUsedVars(cond: Cond) : Set[String] = cond match {
    case ComparisonCond(lhs, op, rhs) => collectUsedVars(lhs) ++ collectUsedVars(rhs)
    case CompoundCond(lhs, op, rhs)   => collectUsedVars(lhs) ++ collectUsedVars(rhs)
    case NegationCond(c)              => collectUsedVars(c)
  }

  def collectUsedVars(body: Body) : Set[String] = body match {
    case a: BodyAtom => a.terms flatMap collectUsedVars toSet
    case a: QuantifiedBody => a.bodies flatMap collectUsedVars toSet
    case a: Cond => collectUsedVars(a)
  }

  def collectDefinedVars(body: Body) : Set[String] = body match {
    case a: BodyAtom => a.terms flatMap collectDefinedVars toSet
    case a: QuantifiedBody => a.bodies flatMap collectDefinedVars toSet
    case a: Cond => Set()
  }

  // check if variables have bindings in the body
  def checkVariableBindings(stmt: Statement) {
    // check variable bindings in a conjunctive query
    def checkCq(cq: ConjunctiveQuery, additionalUsedVars: Set[String] = Set()) {
      // collect variable definitions and usages
      var varDefs = cq.bodies flatMap (_ flatMap collectDefinedVars) toSet
      val varUses = (cq.bodies flatMap (_ flatMap collectUsedVars) toSet) ++
        (cq.headTerms flatMap collectUsedVars) ++
        (additionalUsedVars)
      val varUndefined = varUses -- varDefs
      if (!varUndefined.isEmpty) error(stmt, varUndefined map { x => s"Variable ${x} does not have bindings" } mkString("\n"))
    }
    stmt match {
      case s: ExtractionRule => checkCq(s.q)
      case s: InferenceRule  => checkCq(s.q, (s.weights.variables flatMap collectUsedVars toSet))
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