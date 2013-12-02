package org.deepdive.context

import scala.math.Ordering

object RelationTaskOrdering extends Ordering[Relation] {
  def compare(a: Relation, b: Relation) : Int = {
    val parentRelationsA = Settings.getRelationParents(a.name)
    val parentRelationsB = Settings.getRelationParents(b.name)

    if (parentRelationsA.contains(b.name.toLowerCase)) {
      return 1
    } else if (parentRelationsB.contains(a.name.toLowerCase)) {
      return -1
    } else {
      return 0
    }
  }
}