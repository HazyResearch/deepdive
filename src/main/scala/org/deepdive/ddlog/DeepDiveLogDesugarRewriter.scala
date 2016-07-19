package org.deepdive.ddlog

import scala.language.postfixOps

object DeepDiveLogDesugarRewriter {

  // Finds a safe prefix that starts with given name that is guaranteed to have no collisions with other namesInUse
  def findSafePrefix(name: String, namesInUse: Set[String], minLength: Int = 1, separator: String = "_"): String =
    Stream.from(minLength) map { n =>
      s"${name}${separator * n}"
    } dropWhile { prefix =>
      namesInUse exists {_ startsWith prefix}
    } head

  // Rewrite function call rules whose output coincides with normal rules.
  def desugarUnionsImpliedByFunctionCallRules(program: DeepDiveLog.Program) = {
    def indexByFirst[a,b](pairs: Seq[(a,b)]): Map[a,List[b]] =
      pairs groupBy { _._1 } mapValues { _ map (_._2) toList }

    val schemaByName = indexByFirst(program collect {
      case decl: SchemaDeclaration => decl.a.name -> decl
    }) mapValues (_ head)
    val rulesWithIndexByName = indexByFirst(program.zipWithIndex collect {
      case (fncall: FunctionCallRule, i) => fncall.output -> (fncall, i)
      case (rule  : ExtractionRule  , i) => rule.headName -> (rule  , i)
    })
    val relationNamesUsedInProgram = program collect {
      case decl: SchemaDeclaration  => decl.a.name
      case fncall: FunctionCallRule => fncall.output
      case rule: ExtractionRule     => rule.headName
    } toSet

    // find names that have multiple function calls or mixed type rules
    val relationsToDesugar = rulesWithIndexByName flatMap {
      case (name, allRules) =>
        val (fncalls, rules) = allRules map {_._1} partition {_.isInstanceOf[FunctionCallRule]}
        if ((fncalls size) > 1 || ((fncalls size) > 0 && (rules size) > 0)) {
          Some(name)
        } else None
    }
    val rulesToRewrite = relationsToDesugar flatMap rulesWithIndexByName map {
      _._1} filter {_.isInstanceOf[FunctionCallRule]} toList

    // determine a separator that does not create name clashes with existing heads for each relation to rewrite
    val prefixForRelation: Map[String, String] = relationsToDesugar map { name =>
      name -> (findSafePrefix(name, relationNamesUsedInProgram))
    } toMap

    // how to make names unique
    def makeUnique(name: String, ordLocal: Int, ordGlobal: Int): String = {
      s"${prefixForRelation(name)}${ordLocal}"
    }

    // plan the rewrite
    val rewritePlan : Map[Statement, List[Statement]] =
      program collect {
        // only function call rule needs to be rewritten
        case fncall: FunctionCallRule if rulesToRewrite contains fncall =>
          val relationName: String = fncall.output
          val rulesForTheRelationToRewriteOrdered = rulesWithIndexByName(relationName
            ) sortBy {_._2} filter {_._1.isInstanceOf[FunctionCallRule]}
          val orderAmongRulesToRewrite = rulesForTheRelationToRewriteOrdered map {_._1} indexOf(fncall)
          val orderInProgram = rulesForTheRelationToRewriteOrdered(orderAmongRulesToRewrite)._2
          val nameUnique: String = makeUnique(relationName, orderAmongRulesToRewrite, orderInProgram)
          val schema = schemaByName(relationName)
          fncall -> List(
            schema.copy(a = schema.a.copy(name = nameUnique)),
            fncall.copy(output = nameUnique),
            ExtractionRule(
              headName = relationName,
              q = ConjunctiveQuery(
                headTerms = schema.a.terms map VarExpr,
                bodies = List(List(Atom(name = nameUnique, terms = schema.a.terms map VarPattern)))
              )
            )
          )
          // TODO add union after the last or first or somewhere
      } toMap

    // apply rewrite plan
    program flatMap { case rule => rewritePlan getOrElse(rule, List(rule)) }
  }


  def derive(program: DeepDiveLog.Program): DeepDiveLog.Program = {
    (List(
        desugarUnionsImpliedByFunctionCallRules(_)
      ) reduce (_.compose(_))
    )(program)
  }
}
