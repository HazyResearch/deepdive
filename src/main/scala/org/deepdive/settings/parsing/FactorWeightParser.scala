package org.deepdive.settings

import scala.util.parsing.combinator.RegexParsers
import org.deepdive.Context

object FactorWeightParser extends RegexParsers {
	def relationOrField = """[^,()]+""".r
	def weightVariable = relationOrField

	def constantWeight = """-?[\d\.]+""".r ~ ("[" ~> "[0-9]+".r <~"]").? ^^ { 
		case (number1 ~ Some(number2)) => KnownFactorWeightVector(number1.toDouble,number2.toInt)
		case (number1 ~ _) => KnownFactorWeight(number1.toDouble)
	} 
	def unknownWeight = "?" ~> ("(" ~> repsep(weightVariable, ",") <~ ")").? ~ ("[" ~> "[0-9]+".r <~"]").? ^^ {
		case (Some(varList) ~ Some(number)) => UnknownFactorWeightVector(varList.toList,number.toInt)
		case (Some(varList) ~ _ )=> UnknownFactorWeight(varList.toList)
		case _ => UnknownFactorWeight(List())
	}

	def factorWeightObj = constantWeight | unknownWeight

	def factorWeight = rep1sep(factorWeightObj, ",").? ^^ {
		case Some(weightList) => FactorWeightInit(weightList.toList)
	}

}
