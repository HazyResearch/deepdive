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

    // New head
    val incCqHead = if (isInference) {
      cq.head.copy(
        name = newPrefix + cq.head.name
      )
    } else {
      cq.head.copy(
        name = deltaPrefix + cq.head.name
      )
    }

    var incCqBodies = new ListBuffer[List[Body]]()
    // New incremental bodies
    cq.bodies foreach { bodies =>
      // Delta body
      val incDeltaBody = bodies map { 
        case a: Atom => a.copy(name = deltaPrefix + a.name)
        case a: Cond => a
      }
      // New body
      val incNewBody = bodies map {
        case a: Atom => a.copy(name = newPrefix + a.name)
        case a: Cond => a
      }
      val bodyAtoms         = bodies       collect { case a: Atom => a }
      val bodyConds         = bodies       collect { case a: Cond => a }
      val incDeltaBodyAtoms = incDeltaBody collect { case a: Atom => a }
      val incNewBodyAtoms   = incNewBody   collect { case a: Atom => a }

      var i = 0
      var j = 0
      var index = if (incrementalFunctionInput contains incCqHead.name) -1 else 0
      if (mode == "inc") {
        incCqBodies += incNewBody
      } else {
        for (i <- index to (bodyAtoms.length - 1)) {
          var newBody = new ListBuffer[Body]()
          for (j <- 0 to (bodyAtoms.length - 1)) {
            if (j > i)
              newBody += bodyAtoms(j)
            else if (j < i)
              newBody += incNewBodyAtoms(j)
            else if (j == i)
              newBody += incDeltaBodyAtoms(j)
          }
          newBody = newBody ++ bodyConds
          incCqBodies += newBody.toList
        }
      }
    }
    cq.copy(head = incCqHead, bodies = incCqBodies.toList)
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
        name = deltaPrefix + stmt.a.name
      )
    )
    incrementalStatement += incDeltaStmt

    // New table
    var incNewStmt = stmt.copy(
      a = stmt.a.copy(
        name = newPrefix + stmt.a.name
      )
    )
    incrementalStatement += incNewStmt

    // if (!stmt.isQuery) {
    incrementalStatement += ExtractionRule(ConjunctiveQuery(
      Atom(incNewStmt.a.name, incNewStmt.a.terms map { VarExpr(_) } ),
      List(List(Atom(stmt.a.name, stmt.a.terms map { VarExpr(_) })),
        List(Atom(incDeltaStmt.a.name, incDeltaStmt.a.terms map { VarExpr(_) }))),
      false, None))
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
    List(stmt.copy(q = transform(stmt.q, true, stmt.mode), mode = null))
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
