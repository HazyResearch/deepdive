package org.deepdive.extraction

import org.deepdive.context.Settings
import org.deepdive.context.RelationTaskOrdering
import scala.math.Ordering

object ExtractionTaskOrdering extends Ordering[ExtractionTask] {
  def compare(a: ExtractionTask, b: ExtractionTask) : Int = {
    RelationTaskOrdering.compare(
      Settings.getRelation(a.outputRelation).orNull, 
      Settings.getRelation(b.outputRelation).orNull
    )
  }
}