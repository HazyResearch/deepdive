package org.deepdive.datastore

import org.deepdive.Logging
import org.deepdive.context.{Relation}
import spray.json._
import anorm._

object Utils extends Logging {
  type AnormSeq = Seq[(String, ParameterValue[_])]  

  /* 
   * Converts a JSON array row to a object that can be handled by Anorm
   * Example: ["John", 15, True] => AnormSeq(...)
   */
  implicit def jsonRowToAnormSeq(relationRow: (Relation, JsArray)) : AnormSeq = {
    val (relation, row) = relationRow;
    jsonRowsToAnormSeq((relation, List(row))).head
  }

  /* 
   * Converts a multiple JSON array rows to a object that can be handled by Anorm
   * Example: [["John", 15, True], ...] => Seq[AnormSeq(...), ...]
   */
  implicit def jsonRowsToAnormSeq[T <% Iterable[JsArray]]
    (relationRows: (Relation, T)): Seq[AnormSeq] = {
    val (relation, rows) = relationRows;
    val keys = relation.schema.keys.filterNot(_ == "id")
    val domain = relation.schema.filterKeys(_ != "id").values.toList

    rows.map { row =>
      val anormSeq = row.elements.zip(domain).map {
        case (JsNull, _) => toParameterValue(null)
        case (x : JsString, "String") => toParameterValue(x.value)
        case (x : JsString, "Text") => toParameterValue(x.value)
        case (x : JsNumber, "Integer") => toParameterValue(x.value.toLong)
        case (x : JsBoolean, "Boolean") => toParameterValue(x.value)
        case (value, domain) => 
          log.error(s"Expected value of type ${domain}, but got JSON type ${value.getClass}")
          toParameterValue(null)
      }
      keys.zip(anormSeq).toSeq
    }.toSeq
  }
}
