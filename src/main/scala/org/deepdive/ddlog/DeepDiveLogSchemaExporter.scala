package org.deepdive.ddlog

import org.apache.commons.lang3.StringEscapeUtils
import org.deepdive.ddlog.DeepDiveLog.Mode._

import scala.util.parsing.json.{JSONArray, JSONObject}

// Schema exporter that dumps column names and types of every relation as well as their annotations
object DeepDiveLogSchemaExporter extends DeepDiveLogHandler {

  def jsonMap(kvs: (String, Any)*): Map[String, Any] = kvs toMap

  def export(decl: SchemaDeclaration): (String, JSONObject) = {
    var schema = jsonMap(
//      "name" -> decl.a.name // XXX redundant but potentially useful
    )
    // column names with types and annotations
    schema += "columns" -> JSONObject(
      decl.a.terms.zipWithIndex map {
        case (name, i) =>
          var columnSchema = jsonMap(
//              "name" -> name, // XXX redundant but potentially useful
              "type" -> decl.a.types(i)
          )
          columnSchema += "index" -> i
          // column annotations are omitted when not present
          val annos = decl.a.annotations(i)
          if (annos nonEmpty)
            columnSchema += "annotations" -> exportAnnotations(annos)

          (name, JSONObject(columnSchema))
      } toMap)
    // relation annotations are omitted when not present
    if (decl.annotation nonEmpty)
      schema += "annotations" -> exportAnnotations(decl.annotation)
    // what type of random variable this relation is
    if (decl.isQuery)
      decl.variableType getOrElse { BooleanType } match {
        case ty =>
          schema += "variable_type" -> (ty match {
            case BooleanType        => "boolean"
            case MultinomialType(n) => "multinomial"
          })
          schema += "variable_cardinality" -> ty.cardinality
      }

    // finally, mapping for this relation
    decl.a.name -> JSONObject(schema)
  }

  def exportAnnotations(annos: Seq[Annotation]): JSONArray =
    JSONArray(annos map export toList)

  def export(anno: Annotation): JSONObject = {
    var a = jsonMap(
      "name" -> anno.name
    )
    if (anno.args nonEmpty)
      a += "args" -> (anno.args.get fold (JSONObject, JSONArray))
    JSONObject(a)
  }

  override def run(parsedProgram: DeepDiveLog.Program, config: DeepDiveLog.Config) = {
    val program = parsedProgram  // TODO derive the program based on config.mode?

    println(JSONObject(Map(
      "relations" -> JSONObject(program collect {
          case decl: SchemaDeclaration => export(decl)
        } toMap)
    )))
  }
}
