package org.deepdive.ddlog

import scala.language.postfixOps
import scala.util.parsing.json.{JSONArray, JSONObject}

// Schema exporter that dumps column names and types of every relation as well as their annotations
object DeepDiveLogSchemaExporter extends DeepDiveLogHandler {

  def jsonMap(kvs: (String, Any)*): Map[String, Any] = kvs toMap

  def export(decl: SchemaDeclaration, extra: (String, Any)*): (String, JSONObject) = {
    var schema = jsonMap(extra:_*)
    // column names with types and annotations
    schema += "columns" -> JSONObject(
      decl.a.terms.zipWithIndex map {
        case (name, i) =>
          var columnSchema = jsonMap(
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
    if (decl.annotations nonEmpty)
      schema += "annotations" -> exportAnnotations(decl.annotations)
    // what type of random variable this relation is
    if (decl.isQuery)
      schema += "variable_type" -> (
        if (decl.categoricalColumns.size > 0) "categorical"
        else "boolean"
      )

    // finally, mapping for this relation
    decl.a.name -> JSONObject(schema)
  }

  def exportAnnotations(annos: Seq[Annotation]): JSONArray =
    JSONArray(annos map export toList)

  def export(anno: Annotation): JSONObject = {
    var a = jsonMap(
      "name" -> anno.name
    )
    if (anno.args nonEmpty) {
      def toValue(e: Expr): Any = e match {
        case IntConst(v) => v
        case StringConst(v) => v
        case DoubleConst(v) => v
        case BooleanConst(v) => v
        case NullConst() => null
        case _ => sys.error(s"Cannot export non-constant value in annotation: ${DeepDiveLogPrettyPrinter.printAnnotations(List(anno))}")
      }
      def toJSONObject(nameExprMap: Map[String,Expr]) = {
        JSONObject (nameExprMap mapValues toValue)
      }
      def toJSONArray(exprs: List[Expr]) = JSONArray(exprs map toValue)
      a += "args" -> (anno.args.get fold (toJSONObject, toJSONArray))
    }
    JSONObject(a)
  }

  override def run(parsedProgram: DeepDiveLog.Program, config: DeepDiveLog.Config) = {
    var programToExport = parsedProgram  // TODO derive the program based on config.mode?

    // first find out names of the relations that have SchemaDeclaration
    val declaredNames = programToExport collect {
      case decl: SchemaDeclaration => decl.a.name
    }

    // then print schema in JSON
    println(JSONObject(Map(
      "relations" -> JSONObject(programToExport collect {
          case decl: SchemaDeclaration => export(decl)
          case rule: ExtractionRule if ! (declaredNames contains rule.headName) =>
            // for extraction rules whose head is not declared already,
            // make sure a schema entry is added
            export(SchemaDeclaration(
                a = Attribute(
                  name = rule.headName,
                  terms = rule.q.headTerms.indices map { i => s"column_${i}" } toList,
                  types = rule.q.headTerms map { _ => "UNKNOWN" },
                  annotations = rule.q.headTerms map { _ => List.empty }
                ),
                isQuery = false
              ),
              "type" -> "view"
            )
        } toMap)
    )))
  }
}
