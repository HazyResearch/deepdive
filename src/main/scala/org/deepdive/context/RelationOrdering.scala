package org.deepdive.context

import scala.math.Ordering
import org.deepdive.settings._

object RelationTaskOrdering extends Ordering[Relation] {
  def compare(a: Relation, b: Relation) : Int = {
    val parentRelationsA = Context.settings.findRelationDependencies(a.name) - a.name
    val parentRelationsB = Context.settings.findRelationDependencies(b.name) - b.name

    if (parentRelationsA.contains(b.name.toLowerCase)) {
      return 1
    } else if (parentRelationsB.contains(a.name.toLowerCase)) {
      return -1
    } else {
      return 0
    }
  }
}