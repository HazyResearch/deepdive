package org.deepdive.extraction

import org.deepdive.context._
import org.deepdive.settings._
import org.deepdive.context.RelationTaskOrdering
import scala.math.Ordering

object ExtractionTaskOrdering extends Ordering[ExtractionTask] {
  def compare(a: ExtractionTask, b: ExtractionTask) : Int = {
    RelationTaskOrdering.compare(
      Context.settings.findRelation(a.outputRelation).orNull, 
      Context.settings.findRelation(b.outputRelation).orNull
    )
  }
}