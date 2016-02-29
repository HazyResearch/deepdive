package org.deepdive.ddlog

import org.deepdive.ddlog.DeepDiveLog.{Config, Program}

import scala.collection.mutable.ListBuffer


object Mode extends Enumeration {
  type Mode = Value
  // NewInference: a new inference rule
  // NewFunction: a new function call
  // Normal: existing rules
  val NewInference, NewFunction, Normal = Value
}
import Mode._

object DeepDiveLogDeltaDeriver extends DeepDiveLogHandler {

  // Default prefix for incremental tables
  val deltaPrefix = "dd_delta_"
  val newPrefix = "dd_new_"

  def transform(stmt: Statement): List[Statement] = stmt match {
    case s: SchemaDeclaration   => transform(s)
    case s: FunctionDeclaration => transform(s)
    case s: ExtractionRule      => transform(s)
    case s: FunctionCallRule    => transform(s)
    case s: InferenceRule       => transform(s)
  }

  def transform(headName: String, cq: ConjunctiveQuery, mode: Mode): ConjunctiveQuery = {

    // New head
    val incCqHead = transform(headName)

    var incCqBodies = new ListBuffer[List[Body]]()
    // New incremental bodies
    cq.bodies foreach { bodies =>
      // We don't support deriving delta rules for modifiers
      bodies foreach {
        case _: Cond | _: BodyAtom => // supported
        case _ => throw new RuntimeException("Deriving delta rules for modifier atom is not supported!")
      }
      // Delta body
      val incDeltaBody = bodies collect {
        case a: BodyAtom => a.copy(name = deltaPrefix + a.name)
        case a: Cond => a
      }
      // New body
      val incNewBody = bodies collect {
        case a: BodyAtom => a.copy(name = newPrefix + a.name)
        case a: Cond => a
      }
      val bodyAtoms         = bodies       collect { case a: BodyAtom => a }
      val bodyConds         = bodies       collect { case a: Cond => a }
      val incDeltaBodyAtoms = incDeltaBody collect { case a: BodyAtom => a }
      val incNewBodyAtoms   = incNewBody   collect { case a: BodyAtom => a }

      if (mode == NewInference) {
        incCqBodies += incNewBody
      } else {
        var i = 0
        var j = 0
        val index = if (mode == NewFunction) -1 else 0
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

    cq.copy(bodies = incCqBodies.toList)
  }

  def transform(headName: String) : String = deltaPrefix + headName

  def transform(a: HeadAtom) : HeadAtom = a.copy(name = newPrefix + a.name)

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

    incrementalStatement += ExtractionRule(incNewStmt.a.name,
      ConjunctiveQuery(incNewStmt.a.terms map { VarExpr(_) },
      List(List(BodyAtom(stmt.a.name, stmt.a.terms map { VarPattern(_) })),
      List(BodyAtom(incDeltaStmt.a.name, incDeltaStmt.a.terms map { VarPattern(_) }))),
      false, None))

    incrementalStatement.toList
  }

  // Incremental function declaration,
  // create one delta function scheme based on original function scheme
  def transform(stmt: FunctionDeclaration): List[Statement] = {
    List(stmt.copy(
      inputType = stmt.inputType match {
        case inTy: RelationTypeDeclaration => inTy
        case inTy: RelationTypeAlias =>
          inTy.copy(likeRelationName = deltaPrefix + inTy.likeRelationName)
      },
      outputType = stmt.outputType match {
        case outTy: RelationTypeDeclaration => outTy
        case outTy: RelationTypeAlias =>
          outTy.copy(likeRelationName = deltaPrefix + outTy.likeRelationName)
      }
    ))
  }

  // Incremental extraction rule,
  // create delta rules based on original extraction rule
  def transform(stmt: ExtractionRule): List[Statement] = {
    List(stmt.copy(
      headName = transform(stmt.headName),
      q = transform(stmt.headName, stmt.q, Normal)))
  }

  // Incremental function call rule,
  // modify function input and output
  def transform(stmt: FunctionCallRule): List[Statement] = {
    val mode = if (stmt.mode == Some("inc")) NewFunction else Normal
    List(stmt.copy(
      output = transform(stmt.output),
      q = transform(stmt.output, stmt.q, mode),
      mode = None))
  }

  // Incremental inference rule,
  // create delta rules based on original extraction rule
  def transform(stmt: InferenceRule): List[Statement] = {
    val mode = if (stmt.mode == Some("inc")) NewInference else Normal
    List(stmt.copy(head = stmt.head.copy(terms = stmt.head.terms map transform),
      q = transform("", stmt.q, mode), mode = None))
  }

  def derive(program: DeepDiveLog.Program): DeepDiveLog.Program = {
    var incrementalProgram = new ListBuffer[Statement]()

    for (x <- program) {
      incrementalProgram = incrementalProgram ++ transform(x)
    }
    incrementalProgram.toList
  }

  override def run(program: Program, config: Config): Unit = {
    DeepDiveLogPrettyPrinter.run(derive(program), config)
  }

}
