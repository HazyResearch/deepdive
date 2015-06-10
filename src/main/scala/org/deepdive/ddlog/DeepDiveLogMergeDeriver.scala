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
        name = newPrefix + stmt.a.name,
        terms = stmt.a.terms map {term => term.copy(relName = newPrefix + term.relName)},
        types = stmt.a.types
      )
    )

    ExtractionRule(ConjunctiveQuery(Atom(stmt.a.name, stmt.a.terms),
      List(List(Atom(incNewStmt.a.name, incNewStmt.a.terms)))))
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
