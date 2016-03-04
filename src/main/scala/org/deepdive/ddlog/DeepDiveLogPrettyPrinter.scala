package org.deepdive.ddlog

import org.apache.commons.lang3.StringEscapeUtils

import scala.language.postfixOps

// Pretty printer that simply prints the parsed input
object DeepDiveLogPrettyPrinter extends DeepDiveLogHandler {

  // Dispatch to the corresponding function
  def print(stmt: Statement): String = stmt match {
    case s: SchemaDeclaration   => print(s)
    case s: FunctionDeclaration => print(s)
    case s: ExtractionRule      => print(s)
    case s: FunctionCallRule    => print(s)
    case s: SupervisionRule     => print(s)
    case s: InferenceRule       => print(s)
  }

  def printAnnotations(annos: List[Annotation], indentation: String = ""): String =
    annos map {
      case anno =>
        s"@${anno.name}${
          if (anno.args isEmpty) ""
          else s"(${anno.args.get fold (
            // named arguments case
            {_ map { case (name, value) => s"${name}=${print(value)}" }},
            // unnamed argument list case
            {_ map print}
          ) mkString(", ") })"
        }" + ("\n" + indentation)
    } mkString


  def print(stmt: SchemaDeclaration): String = {
    val prefix = s"${stmt.a.name}${if (stmt.isQuery) "?" else ""}("
    val indentation = " " * prefix.length
    val columnDecls = stmt.a.terms.zipWithIndex map {
      case (name,i) =>
        printAnnotations(stmt.a.annotations(i), indentation) +
        s"${name} ${stmt.a.types(i)}"
    }
    printAnnotations(stmt.annotations) +
    s"""${prefix}${columnDecls.mkString(",\n" + indentation)}).
       |""".stripMargin
  }

  def print(functionInputOutputType: FunctionInputOutputType): String = functionInputOutputType match {
    case ty: RelationTypeAlias => s"rows like ${ty.likeRelationName}"
    case ty: RelationTypeDeclaration =>
      val namesWithTypes = (ty.names, ty.types).zipped map {
        (colName,colType) => s"${colName} ${colType}"}
      s"(${namesWithTypes.mkString(", ")})"
  }
  def print(stmt: FunctionDeclaration): String = {
    val inputType = print(stmt.inputType)
    val outputType = print(stmt.outputType)
    val impls = stmt.implementations map {
      case impl: RowWiseLineHandler => {
        val styleStr = if (impl.style == "plpy") s"\n        runs as plpy"
        else s"\n        handles ${impl.style} lines"
        "\"" + StringEscapeUtils.escapeJava(impl.command) + "\"" + styleStr
      }
    }
    printAnnotations(stmt.annotations) +
    s"""function ${stmt.functionName}
       |    over ${inputType}
       | returns ${outputType}
       | ${(impls map {"implementation " + _}).mkString("\n ")}.
       |""".stripMargin
  }

  def printLiteral(value: Any): String = value match {
    case s: String => "\"" + StringEscapeUtils.escapeJava(s) + "\""
    case _ => value toString
  }

  // print an expression
  def print(e: Expr) : String = print(e, 0)
  def print(e: Expr, level: Int) : String = {
    e match {
      case VarExpr(name) => name
      case NullConst() => "NULL"
      case IntConst(value) => value.toString
      case DoubleConst(value) => value.toString
      case BooleanConst(value) => value.toString
      case StringConst(value) => "\"" + StringEscapeUtils.escapeJava(value) + "\""
      case FuncExpr(function, args, agg) => {
        val resolvedArgs = args map (x => print(x))
        s"${function}(${resolvedArgs.mkString(", ")})"
      }
      case BinaryOpExpr(lhs, op, rhs) => {
        val p = s"${print(lhs, level + 1)} ${op} ${print(rhs, level + 1)}"
        if (level == 0) p else s"(${p})"
      }
      case TypecastExpr(lhs, rhs) => s"(${print(lhs)} :: ${rhs})"
      case IfThenElseExpr(ifCondExprPairs, optElseExpr) => (
        (ifCondExprPairs map {
          case (ifCond, thenExpr) => s"if ${print(ifCond)} then ${print(thenExpr)}"
        }) ++ (optElseExpr map print toList)
      ) mkString("", "\n\telse ", "\n\tend")
      case ArrayElementExpr(array, index) => s"${print(array, level + 1)}[${print(index, level + 1)}]"
    }
  }

  def print(a: Atom) : String = {
    val vars = a.terms map print
    s"${a.name}(${vars.mkString(", ")})"
  }

  def print(p: Pattern) : String = p match {
    case VarPattern(x) => x
    case PlaceholderPattern() => "_"
    case ExprPattern(e) => print(e)
  }

  def print(a: QuantifiedBody) : String = {
    val modifier = a.modifier match {
      case ExistModifier(negated) => if(negated) "NOT " else "" + "EXISTS"
      case OuterModifier() => "OPTIONAL"
      case AllModifier() => "ALL"
    }
    val bodyStr = (a.bodies map print).mkString(", ")
    s"${modifier}[${bodyStr}]"
  }

  // print a condition
  def print(cond: Cond) : String = {
    cond match {
      case ExprCond(e) => print(e)
      case NegationCond(c) => s"[!${print(c)}]"
      case CompoundCond(lhs, op, rhs) => {
        op match {
          case LogicOperator.AND() => s"[${print(lhs)}, ${print(rhs)}]"
          case LogicOperator.OR()  => s"[${print(lhs)}; ${print(rhs)}]"
        }
      }
    }
  }

  def print(b: Body) : String = b match {
    case b: Atom => print(b)
    case b: Cond => print(b)
    case b: QuantifiedBody => print(b)
  }

  def print(cq: ConjunctiveQuery, optSupervision: Option[Expr] = None): String = {

    def printBodyList(b: List[Body]) = {
      s"${(b map print).mkString(",\n    ")}"
    }

    val bodyStr = (cq.bodies map printBodyList).mkString(";\n    ")

    val distinctStr = if (cq.isDistinct) " *" else ""
    val limitStr    = cq.limit map { " | " + _ } getOrElse("")
    val headStrTmp  = cq.headTerms map print mkString(", ")
    val headStr     = if (headStrTmp isEmpty) "" else s"(${headStrTmp})"

    val headBodySeparator = if (cq.isForQuery) "?-" else ":-"
    headStr + (
      optSupervision map { sv => s" = ${print(sv)}" } getOrElse("")
    ) + distinctStr + limitStr + s" ${headBodySeparator}\n    " + bodyStr
  }

  def print(a: HeadAtom) : String = {
    s"${if (a.isNegated) "!" else ""}${print(a.atom)}"
  }

  def print(head: InferenceRuleHead) : String = {
    def printImplyHead = s"""${printHead(head.variables.dropRight(1), ", ")} => ${print(head.variables.last)}"""
    def printHead(a: List[HeadAtom], delim: String) : String = a map print mkString(delim)
    head.function match {
      case FactorFunction.Imply()  => printImplyHead
      case FactorFunction.Linear() => printImplyHead
      case FactorFunction.Ratio()  => printImplyHead
      case FactorFunction.And()    => printHead(head.variables, " ^ ")
      case FactorFunction.Or()     => printHead(head.variables, " v ")
      case FactorFunction.Equal()  => printHead(head.variables, " = ")
      case FactorFunction.IsTrue() => printHead(head.variables, "")
    }
  }

  def print(stmt: ExtractionRule): String = {
    printAnnotations(stmt.annotations) +
    stmt.headName + print(stmt.q) + ".\n"
  }

  def print(stmt: SupervisionRule): String = {
    printAnnotations(stmt.annotations) +
    stmt.headName + print(stmt.q, Some(stmt.supervision)) + ".\n"
  }

  def print(stmt: FunctionCallRule): String = {
    printAnnotations(stmt.annotations) +
    s"${stmt.output} += ${stmt.function}${print(stmt.q)}.\n"
  }

  def print(stmt: InferenceRule): String = {
    printAnnotations(stmt.annotations) +
    print(stmt.head) + print(stmt.q) + ".\n"
  }

  override def run(parsedProgram: DeepDiveLog.Program, config: DeepDiveLog.Config) = {
    val programToPrint = parsedProgram
    // pretty print in original syntax
    programToPrint foreach { stmt => println(print(stmt)) }
  }
}
