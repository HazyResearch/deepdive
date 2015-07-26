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
      case s: FunctionCallRule => checkRelation(BodyAtom(s.input, Nil))
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
    def checkHeadAtom(a: HeadAtom) = check(a.name, a.terms.size)
    def checkCq(cq: ConjunctiveQuery) {
      checkHeadAtom(cq.head)
      cq.bodies foreach checkBodyAtoms(checkBodyAtom)
    }
    stmt match {
      case s: ExtractionRule => checkCq(s.q)
      case s: InferenceRule  => checkCq(s.q)
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
  def collectVariables(expr: Expr) : Set[String] = {
    var set = new HashSet[String]()
    def collectVariablesInner(e: Expr) : Unit = e match {
      case VarExpr(name) => set += name
      case FuncExpr(function, args, agg) => args foreach (collectVariablesInner(_))
      case BinaryOpExpr(lhs, op, rhs) => {
        collectVariablesInner(lhs)
        collectVariablesInner(rhs)
      }
      case TypecastExpr(lhs, rhs) => collectVariablesInner(lhs)
      case _ =>
    }
    collectVariablesInner(expr)
    set
  }

  def collectVariables(pattern: Pattern, definitionOnly: Boolean) : Set[String] = {
    var set = new HashSet[String]()
    pattern match {
      case VarPattern(name) => set += name
      case ExprPattern(e) => if (!definitionOnly) set ++= collectVariables(e)
      case PlaceholderPattern() =>
    }
    set
  }

  def collectVariables(cond: Cond) : Set[String] = {
    var set = new HashSet[String]()
    cond match {
      case ComparisonCond(lhs, op, rhs) => {
        set ++= collectVariables(lhs)
        set ++= collectVariables(rhs)
      }
      case CompoundCond(lhs, op, rhs) => {
        set ++= collectVariables(lhs)
        set ++= collectVariables(rhs)
      }
      case NegationCond(c) => set ++= collectVariables(c)
    }
    set
  }

  // collect variable
  def collectVariablesFromBody(body: Body, definitionOnly: Boolean) : Set[String] = {
    var set = new HashSet[String]()
    body match {
      case a: BodyAtom => a.terms.foreach { x =>
        set ++= collectVariables(x, definitionOnly)
      }
      case a: QuantifiedBody => a.bodies.foreach {
        x => set ++= collectVariablesFromBody(x, definitionOnly)
      }
      case a: Cond => if (!definitionOnly) set ++= collectVariables(a)
    }
    set
  }

  // check if variables have bindings in the body
  def checkVariableBindings(stmt: Statement) {
    // check variable bindings in a conjunctive query
    def checkCq(cq: ConjunctiveQuery, additionalVars: Set[String] = Set()) {
      // collect variable definitions and usages
      var varDefSet = new HashSet[String]()
      var varUseSet = new HashSet[String]()
      cq.bodies.foreach { case x =>
        x.foreach { case a =>
          varDefSet ++= collectVariablesFromBody(a, true)
          varUseSet ++= collectVariablesFromBody(a, false)
        }
      }
      cq.head.terms.foreach  { x => varUseSet ++= collectVariables(x) }
      varUseSet ++= additionalVars
      varUseSet.foreach { x =>
        if (!varDefSet.contains(x)) error(stmt, s"Variable ${x} does not have bindings")
      }
    }
    stmt match {
      case s: ExtractionRule => checkCq(s.q)
      case s: InferenceRule  => {
        var set = new HashSet[String]()
        s.weights.variables.foreach { set ++= collectVariables(_) }
        checkCq(s.q, set)
      }
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