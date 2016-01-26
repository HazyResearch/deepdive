package org.deepdive.ddlog

object DeepDiveLogDesugarRewriter {

  // Rewrite multiple types of rules with same heads as explicit union rules.
  def desugarImplicitUnions(program: DeepDiveLog.Program) = {
    // TODO
    program
  }


  def derive(program: DeepDiveLog.Program): DeepDiveLog.Program = {
    (List(
        desugarImplicitUnions(_)
      ) reduce (_.compose(_))
    )(program)
  }
}
