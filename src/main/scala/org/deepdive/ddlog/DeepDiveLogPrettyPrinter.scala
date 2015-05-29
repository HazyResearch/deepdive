package org.deepdive.ddlog

import org.apache.commons.lang3.StringEscapeUtils
import org.deepdive.ddlog.DeepDiveLog.Mode._

// Pretty printer that simply prints the parsed input
object DeepDiveLogPrettyPrinter extends DeepDiveLogHandler {

  // Dispatch to the corresponding function
  def print(stmt: Statement): String = stmt match {
    case s: SchemaDeclaration   => print(s)
    case s: FunctionDeclaration => print(s)
    case s: ExtractionRule      => print(s)
    case s: FunctionCallRule    => print(s)
    case s: InferenceRule       => print(s)
  }

  def print(stmt: SchemaDeclaration): String = {
    val columnDecls = stmt.a.terms map {
      case Variable(name, _, i) => s"${name} ${stmt.a.types(i)}"
    }
    val prefix = s"${stmt.a.name}${if (stmt.isQuery) "?" else ""}("
    val indentation = " " * prefix.length
    s"""${prefix}${columnDecls.mkString(",\n" + indentation)}).
       |""".stripMargin
  }

  def print(relationType: RelationType): String = relationType match {
    case ty: RelationTypeAlias => s"like ${ty.likeRelationName}"
    case ty: RelationTypeDeclaration =>
      val namesWithTypes = (ty.names, ty.types).zipped map {
        (colName,colType) => s"${colName} ${colType}"}
      s"(${namesWithTypes.mkString(", ")})"
  }
  def print(stmt: FunctionDeclaration): String = {
    val inputType = print(stmt.inputType)
    val outputType = print(stmt.outputType)
    val impls = stmt.implementations map {
      case impl: RowWiseLineHandler =>
        "\"" + StringEscapeUtils.escapeJava(impl.command) + "\"" +
        s"\n        handles ${impl.format} lines"
    }
    val modeStr = if (stmt.mode == null) "" else s" mode = ${stmt.mode}"
    s"""function ${stmt.functionName}
       |    over ${inputType}
       | returns ${outputType}
       | ${(impls map {"implementation " + _}).mkString("\n ")}${modeStr}.
       |""".stripMargin
  }

  def print(cq: ConjunctiveQuery): String = {
    val printAtom = {a:Atom =>
      val vars = a.terms map { _.varName }
      s"${a.name}(${vars.mkString(", ")})"
    }
    val printListAtom = {a:List[Atom] =>
      s"${(a map printAtom).mkString(",\n    ")}"
    }
    s"""${printAtom(cq.head)} :-
       |    ${(cq.bodies map printListAtom).mkString(";\n    ")}""".stripMargin
  }

  def print(stmt: ExtractionRule): String = {
    print(stmt.q) +
    ( if (stmt.supervision == null) ""
      else  "\n  label = " + stmt.supervision
    ) + ".\n"
  }

  def print(stmt: FunctionCallRule): String = {
    s"""${stmt.output} :- !${stmt.function}(${stmt.input}).
       |""".stripMargin
  }

  def print(stmt: InferenceRule): String = {
    print(stmt.q) +
    ( if (stmt.weights == null) ""
      else "\n  weight = " + (stmt.weights match {
        case KnownFactorWeight(w) => w.toString
        case UnknownFactorWeight(vs) => vs.mkString(", ")
      })
    ) +
    ( if (stmt.semantics == null) ""
      else "\n  semantics = " + stmt.semantics
    ) + ".\n"
  }

  override def run(parsedProgram: DeepDiveLog.Program, config: DeepDiveLog.Config) = {
    val programToPrint =
      // derive the program based on mode information
      config.mode match {
        case ORIGINAL => parsedProgram
        case INCREMENTAL => DeepDiveLogDeltaDeriver.derive(parsedProgram)
        case MATERIALIZATION => parsedProgram
        case MERGE => DeepDiveLogMergeDeriver.derive(parsedProgram)
      }
    // pretty print in original syntax
    programToPrint foreach {stmt => println(print(stmt))}
  }
}
