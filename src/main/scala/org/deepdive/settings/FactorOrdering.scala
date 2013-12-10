package org.deepdive.settings

import scala.math.Ordering
import org.deepdive.context._

object FactorOrdering extends Ordering[FactorDesc] {
  def compare(a: FactorDesc, b: FactorDesc) : Int = {
    val aRelation = Context.settings.findRelation(a.relation).orNull
    val bRelation = Context.settings.findRelation(b.relation).orNull
    return RelationTaskOrdering.compare(aRelation, bRelation)
  }
}