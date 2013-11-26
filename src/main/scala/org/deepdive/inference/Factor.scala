package org.deepdive.inference

import anorm._
import org.deepdive.datastore.Utils.AnormSeq

case class Factor(id: Integer, factorFunction: FactorFunction, weight: Weight, variables: List[FactorVariable])
case class FactorVariable(factorId: Long, position: Integer, positive: Boolean, value: Variable)
case class FactorFunction(id: Long, desc: String)

object FactorFunction {
  implicit def toAnormSeq(value: FactorFunction) : AnormSeq = {
    Seq(("id", toParameterValue(value.id)), ("description", toParameterValue(value.desc)))
  }
}

object Factor {
  implicit def toAnormSeq(value: Factor) : AnormSeq = {
    Seq(("id", toParameterValue(value.id)), ("weight_id", toParameterValue(value.weight.id)),
      ("factor_function_id", toParameterValue(value.factorFunction.id)))
  }
}

object FactorVariable {
  implicit def toAnormSeq(value: FactorVariable) : AnormSeq = {
    Seq(("factor_id", toParameterValue(value.factorId)), ("variable_id", toParameterValue(value.value.id)),
      ("position", toParameterValue(value.position)), ("is_positive", toParameterValue(value.positive)))
  }
}