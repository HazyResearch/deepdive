package org.deepdive.ddlog

import org.deepdive.ddlog.DeepDiveLog.{Config, Program}

object DeepDiveLogQueryCompiler extends DeepDiveLogHandler {

  override def run(program: Program, config: Config): Unit = {
    val (query, extraRules) = (new DeepDiveLogParser).parseQuery(config.query)

    // make sure extraRules don't have name clashes with the program
    new DeepDiveLogSemanticChecker(program).checkNoRedefinition(program, extraRules)

    // run typical checks for correct semantics
    val programToCheck = ExtractionRule(q = query, headName = "") :: extraRules ++ program
    DeepDiveLogSemanticChecker.run(programToCheck, config)

    // use schema declarations, etc. in given program
    val compilationState = new CompilationState(
      config = config,
      statements = extraRules ++ program
    )
    val sql = (extraRules collect {
      // compile supporting rules as CREATE TEMP TABLE queries
      case rule: ExtractionRule =>
        val qc = new QueryCompiler(rule.q, compilationState)
        s"""CREATE TEMPORARY TABLE ${rule.headName} AS\n${
          qc.generateSQL(AliasStyle.ViewAlias)}"""
    }) ++ List({
      // compile the query
      s"""${new QueryCompiler(query, compilationState).generateSQL(AliasStyle.UseVariableAsAlias)}"""
    }) mkString(";\n\n")
    println(sql)
  }

}
