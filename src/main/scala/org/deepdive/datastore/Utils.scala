package org.deepdive.datastore

import org.deepdive.Logging
import org.deepdive.settings._
import spray.json._
import anorm._

object Utils extends Logging {
  type AnormSeq = Seq[(String, ParameterValue[_])]  

  /* 
   * Converts a JSON objects row to a object that can be handled by Anorm
   * Example: ["John", 15, True] => AnormSeq(...)
   */
  implicit def jsonRowToAnormSeq(relationRow: (Relation, JsObject)) : AnormSeq = {
    val (relation, row) = relationRow;
    jsonRowsToAnormSeq((relation, List(row))).head
  }

  /* 
   * Converts a multiple JSON objects rows to a object that can be handled by Anorm
   * Example: [["John", 15, True], ...] => Seq[AnormSeq(...), ...]
   */
  implicit def jsonRowsToAnormSeq[T <% Iterable[JsObject]]
    (relationRows: (Relation, T)): Seq[AnormSeq] = {
    val (relation, rows) = relationRows;
    val keys = relation.schema.keys.filterNot(_ == "id")
    val domain = relation.schema.filterKeys(_ != "id")

    rows.map { row =>
      val anormSeq = row.fields.map { case(field, value) =>
        (field, (value, domain.get(field).orNull))
      }.mapValues { 
        case (JsNull, _) => toParameterValue(null)
        case (x : JsString, "String") => toParameterValue(x.value)
        case (x : JsString, "Text") => toParameterValue(x.value)
        case (x : JsNumber, "Integer") => toParameterValue(x.value.toLong)
        case (x : JsBoolean, "Boolean") => toParameterValue(x.value)
        case (value, domain) => 
          log.error(s"Expected value of type ${domain}, but got JSON type ${value.getClass}")
          toParameterValue(null)
      }
      anormSeq.toSeq
    }.toSeq
  }
}
