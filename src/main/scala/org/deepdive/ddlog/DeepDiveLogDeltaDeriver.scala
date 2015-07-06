package org.deepdive.ddlog

import scala.collection.mutable.ListBuffer

object DeepDiveLogDeltaDeriver{

  // Default prefix for incremental tables
  val deltaPrefix = "dd_delta_"
  val newPrefix = "dd_new_"

  var incrementalFunctionInput = new ListBuffer[String]()

  def transform(stmt: Statement): List[Statement] = stmt match {
    case s: SchemaDeclaration   => transform(s)
    case s: FunctionDeclaration => transform(s)
    case s: ExtractionRule      => transform(s)
    case s: FunctionCallRule    => transform(s)
    case s: InferenceRule       => transform(s)
  }

  def transform(cq: ConjunctiveQuery, isInference: Boolean, mode: String): ConjunctiveQuery = {
    // transform an expression
    def transformExpression(expr: Expression, prefix: String) = {
      val newVars = expr.variables map {
        case term: Variable => term.copy(relName = prefix + term.relName)
        case term: Constant => term
      }
      Expression(newVars, expr.ops, expr.relName, expr.index)
    }

    // New head
    val incCqHead = if (isInference) {
      cq.head.copy(
        name = newPrefix + cq.head.name,
        terms = cq.head.terms map (transformExpression(_, newPrefix))
      )
    } else {
      cq.head.copy(
        name = deltaPrefix + cq.head.name,
        terms = cq.head.terms map (transformExpression(_, deltaPrefix))
      )
    }

    var incCqBodies = new ListBuffer[List[Atom]]()
    // New incremental bodies
    for (body <- cq.bodies) {
      // Delta body
      val incDeltaBody = body map {
        a => a.copy(
          name = deltaPrefix + a.name,
          terms = a.terms map (transformExpression(_, deltaPrefix))
        )
      }
      // New body
      val incNewBody = body map {
        a => a.copy(
          name = newPrefix + a.name,
          terms = a.terms map (transformExpression(_, newPrefix))
        )
      }
      var i = 0
      var j = 0
      var index = if (incrementalFunctionInput contains incCqHead.name) -1 else 0
      if (mode == "inc") {
        incCqBodies += incNewBody
      } else {
        for (i <- index to (body.length - 1)) {
          var newBody = new ListBuffer[Atom]()
          for (j <- 0 to (body.length - 1)) {
            if (j > i)
              newBody += body(j)
            else if (j < i)
              newBody += incNewBody(j)
            else if (j == i)
              newBody += incDeltaBody(j)
          }
          incCqBodies += newBody.toList
        }
      }
    }
    // TODO fix conditions
    ConjunctiveQuery(incCqHead, incCqBodies.toList, cq.conditions)
  }

  // Incremental scheme declaration,
  // keep the original scheme and create one delta scheme
  def transform(stmt: SchemaDeclaration): List[Statement] = {
    var incrementalStatement = new ListBuffer[Statement]()
    // Incremental table
    incrementalStatement += stmt

    // Delta table
    var incDeltaStmt = stmt.copy(
      a = stmt.a.copy(
        name = deltaPrefix + stmt.a.name,
        terms = stmt.a.terms map {term => term.copy(relName = deltaPrefix + term.relName)},
        types = stmt.a.types
      )
    )
    incrementalStatement += incDeltaStmt

    // New table
    var incNewStmt = stmt.copy(
      a = stmt.a.copy(
        name = newPrefix + stmt.a.name,
        terms = stmt.a.terms map {term => term.copy(relName = newPrefix + term.relName)},
        types = stmt.a.types
      )
    )
    incrementalStatement += incNewStmt

    // from schema declaration to expressions
    def variableToExpr(v: Variable) = Expression(List(v), List(), v.relName, v.index)
    val originalExpr = stmt.a.terms map (variableToExpr(_))
    val incDeltaExpr = incDeltaStmt.a.terms map (variableToExpr(_))
    val incNewExpr = incNewStmt.a.terms map (variableToExpr(_))

    // if (!stmt.isQuery) {
    incrementalStatement += ExtractionRule(ConjunctiveQuery(
      Atom(incNewStmt.a.name, incNewExpr),
      List(List(Atom(stmt.a.name, originalExpr)), List(Atom(incDeltaStmt.a.name, incDeltaExpr))), 
      List(None)))
    // }
    incrementalStatement.toList
  }

  // Incremental function declaration,
  // create one delta function scheme based on original function scheme
  def transform(stmt: FunctionDeclaration): List[Statement] = {
    List(stmt.copy(
      inputType = stmt.inputType match {
        case inTy: RelationTypeDeclaration => 
          inTy.copy(names = inTy.names map {name => deltaPrefix + name})
        case inTy: RelationTypeAlias => 
          inTy.copy(likeRelationName = deltaPrefix + inTy.likeRelationName)
      },
      outputType = stmt.outputType match {
        case outTy: RelationTypeDeclaration =>
          outTy.copy(names = outTy.names map {name => deltaPrefix + name})
        case outTy: RelationTypeAlias =>
          outTy.copy(likeRelationName = deltaPrefix + outTy.likeRelationName)
      }
    ))
  }

  // Incremental extraction rule,
  // create delta rules based on original extraction rule
  def transform(stmt: ExtractionRule): List[Statement] = {
    // if (stmt.supervision != null)
      // List(ExtractionRule(transform(stmt.q, true, null), stmt.supervision))
    // else
      List(ExtractionRule(transform(stmt.q, false, null), stmt.supervision))
  }

  // Incremental function call rule,
  // modify function input and output
  def transform(stmt: FunctionCallRule): List[Statement] = {
    List(FunctionCallRule(deltaPrefix + stmt.input, deltaPrefix + stmt.output, stmt.function))
  }

  // Incremental inference rule,
  // create delta rules based on original extraction rule
  def transform(stmt: InferenceRule): List[Statement] = {
    List(InferenceRule(transform(stmt.q, true, stmt.mode), stmt.weights, stmt.semantics))
  }

  def generateIncrementalFunctionInputList(program: DeepDiveLog.Program) {
    program.foreach {
      case x:FunctionDeclaration => if (x.mode == "inc") {
        x.inputType match {
          case inTy: RelationTypeAlias => incrementalFunctionInput += deltaPrefix + inTy.likeRelationName
          case _ =>
        }
      }
      case _ =>
    }
  }

  def derive(program: DeepDiveLog.Program): DeepDiveLog.Program = {
    var incrementalProgram = new ListBuffer[Statement]()

    generateIncrementalFunctionInputList(program)

    for (x <- program) {
      incrementalProgram = incrementalProgram ++ transform(x)
    }
    incrementalProgram.toList
  }
}
