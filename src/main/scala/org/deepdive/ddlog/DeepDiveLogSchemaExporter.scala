package org.deepdive.ddlog

import org.apache.commons.lang3.StringEscapeUtils
import org.deepdive.ddlog.DeepDiveLog.Mode._

import scala.util.parsing.json.{JSONArray, JSONObject}

// Schema exporter that dumps column names and types of every relation as well as their annotations
object DeepDiveLogSchemaExporter extends DeepDiveLogHandler {

  def export(decl: SchemaDeclaration): (String, JSONObject) = {
    val schema = JSONObject(Map(
      "columns" -> JSONObject(Map() ++ decl.a.terms.zipWithIndex map {
        case (name, i) => (name, JSONObject(Map(
          "type" -> decl.a.types(i),
          "annotations" -> exportAnnotations(decl.a.annotations(i))
        )))
      }),
      "annotations" -> exportAnnotations(decl.annotation)
    ))
    (decl.a.name, schema)
  }

  def exportAnnotations(annos: Seq[Annotation]): JSONArray =
    JSONArray(annos map export toList)

  def export(anno: Annotation): JSONObject =
    JSONObject(Map(
        "name" -> anno.name,
        "args" -> JSONObject(anno.args)
      ))

  override def run(parsedProgram: DeepDiveLog.Program, config: DeepDiveLog.Config) = {
    val program = parsedProgram  // TODO derive the program based on config.mode?
    val relations = program collect {
      case decl: SchemaDeclaration => export(decl)
    }
    println(JSONObject(Map() ++ relations))
  }
}
