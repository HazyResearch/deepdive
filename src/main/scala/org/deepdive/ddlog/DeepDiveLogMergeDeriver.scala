package org.deepdive.ddlog

import scala.collection.mutable.ListBuffer

object DeepDiveLogMergeDeriver{

  // Default prefix for incremental tables
  val newPrefix = "dd_new_"

  // Incremental scheme declaration,
  // keep the original scheme and create one delta scheme
  def transform(stmt: SchemaDeclaration): Statement = {
    // New table
    var incNewStmt = stmt.copy(
      a = stmt.a.copy(
        name = newPrefix + stmt.a.name
      )
    )

    ExtractionRule(stmt.a.name,
      ConjunctiveQuery(stmt.a.terms map { VarExpr(_) },
      List(List(BodyAtom(incNewStmt.a.name, incNewStmt.a.terms map { VarPattern(_) }))),
      false, None))
  }

  def derive(program: DeepDiveLog.Program): DeepDiveLog.Program = {
    var mergeProgram = new ListBuffer[Statement]()
    program foreach { x =>
      x match {
        case x: SchemaDeclaration => if (!x.isQuery) mergeProgram += transform(x)
        case _ =>
      }
    }
    mergeProgram.toList
  }
}
