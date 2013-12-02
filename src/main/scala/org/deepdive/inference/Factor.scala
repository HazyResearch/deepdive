package org.deepdive.inference

import anorm._
import org.deepdive.datastore.Utils.AnormSeq

case class Factor(id: Integer, factorFunction: String, weight: Weight, variables: List[FactorVariable])
case class FactorVariable(factorId: Long, position: Integer, positive: Boolean, value: Variable)

object Factor {
  implicit def toAnormSeq(value: Factor) : AnormSeq = {
    Seq(("id", toParameterValue(value.id)), ("weight_id", toParameterValue(value.weight.id)),
      ("factor_function", toParameterValue(value.factorFunction)))
  }
}

object FactorVariable {
  implicit def toAnormSeq(value: FactorVariable) : AnormSeq = {
    Seq(("factor_id", toParameterValue(value.factorId)), ("variable_id", toParameterValue(value.value.id)),
      ("position", toParameterValue(value.position)), ("is_positive", toParameterValue(value.positive)))
  }
}