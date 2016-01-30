package org.deepdive.ddlog

import org.deepdive.ddlog.DeepDiveLog.{Config, Program}

object DeepDiveLogQueryCompiler extends DeepDiveLogHandler {

  override def run(program: Program, config: Config): Unit = {
    val (query, extraRules) = (new DeepDiveLogParser).parseQuery(config.query)

    // TODO make sure extraRules don't have name clashes with program
    // TODO run SemanticChecker

    // use schema declarations, etc. in given program
    val compilationState = new CompilationState(
      config = config,
      statements = program ++ extraRules
    )
    val sql = (extraRules collect {
      // compile supporting rules as CREATE TEMP TABLE queries
      case rule: ExtractionRule =>
        val qc = new QueryCompiler(rule.q, compilationState)
        s"""CREATE TEMPORARY TABLE ${rule.headName} AS\n${
          qc.generateSQL(AliasStyle.ViewAlias)}"""
    }) ++ List({
      // compile the query
      s"""${new QueryCompiler(query, compilationState).generateSQL()}"""
    }) mkString(";\n\n")
    println(sql)
  }

}
