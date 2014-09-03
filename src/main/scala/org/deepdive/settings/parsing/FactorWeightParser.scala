package org.deepdive.settings
import org.deepdive.Logging
import java.io.{File, PrintWriter}
import scala.util.parsing.combinator.RegexParsers
import org.deepdive.Context

object FactorWeightParser extends RegexParsers with Logging  {
	def relationOrField = """[^,()]+""".r
	def weightVariable = relationOrField

	def constantWeight = 
	{
		val temp="""-?[\d\.]+""".r ~ ("[" ~> "[0-9]+".r <~"]").? ^^ { 
			case (number1 ~ Some(number2)) => {
				// log.info("+++++ KnownFactorWeightVector: "+number1.toString)
				KnownFactorWeightVector(number1.toDouble,number2.toInt)
			}
			case (number1 ~ _) => {
				// log.info("+++++ KnownFactorWeight: "+number1.toString)
				KnownFactorWeight(number1.toDouble)
			}
		}
		log.info("+++++"+temp.toString)
		temp
	} 
	def unknownWeight = 
	{	val temp="?" ~> ("(" ~> repsep(weightVariable, ",") <~ ")").? ~ ("[" ~> "[0-9]+".r <~"]").? ^^ {
			case (Some(varList) ~ Some(number)) => {
				// log.info("+++++ UnknownFactorWeightVector: "+number.toString)
				UnknownFactorWeightVector(varList.toList,number.toInt)
			}
			case (Some(varList) ~ _ ) => {
				// log.info("+++++ UnknownFactorWeight LIST: "+varList.toString)
				UnknownFactorWeight(varList.toList)
			}
			case _ => {
				// log.info("+++++ UnknownFactorWeight EMPTY")
				UnknownFactorWeight(List())
			}
		}
		// log.info("+++++"+temp.toString)
		temp
		// log.info("constantWeightconstantWeightconstantWeight")
	}

	def factorWeightObj = constantWeight | unknownWeight

	def factorWeight = ("" ~> rep1sep(factorWeightObj, ",") <~"").? ^^ {
		case Some(weightList) => {
			// log.info("+++++ INIT: "+weightList.toList.toString)
			FactorWeightInit(weightList.toList)
		}
	}
}