package org.deepdive.context

import scala.math.Ordering

object FactorTaskOrdering extends Ordering[Relation] {
  def compare(a: Relation, b: Relation) : Int = {
    val parentRelationsA = a.foreignKeys.map(_.parentRelation.toLowerCase).toSet
    val parentRelationsB = a.foreignKeys.map(_.parentRelation.toLowerCase).toSet

    if (parentRelationsA.contains(b.name.toLowerCase)) {
      return 1
    } else if (parentRelationsB.contains(a.name.toLowerCase)) {
      return -1
    } else {
      return 0
    }
  }
}