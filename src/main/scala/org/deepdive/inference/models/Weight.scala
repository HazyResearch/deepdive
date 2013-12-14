package org.deepdive.inference

import anorm._
import org.deepdive.datastore.Utils.AnormSeq

case class Weight(id: Integer, value: Double, isFixed: Boolean)

object Weight {

  implicit def toAnormSeq(weight: Weight) : AnormSeq = {
    Seq(("id", toParameterValue(weight.id)), ("initial_value", toParameterValue(weight.value)),
      ("is_fixed", toParameterValue(weight.isFixed)))
  }

}