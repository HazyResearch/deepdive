package org.deepdive.settings

import org.deepdive.Logging
import scala.util.parsing.combinator.RegexParsers

object FactorFunctionParser extends RegexParsers with Logging {
  def relationOrField = """[\w]+""".r
  def arrayDefinition = """\[\]""".r
  def equalPredicate = """[0-9]+""".r
  // def factorFunctionName = "Imply" | "Or" | "And" | "Equal" | "IsTrue"

  def implyFactorFunction = ("Imply" | "IMPLY") ~> "(" ~> rep1sep(factorVariable, ",") <~ ")" ^^ { varList =>
    ImplyFactorFunction(varList)
  }
// TODO : "Convolution" doesn't work
  def convolutionFactorFunction = ("Conv" | "CONV" | "Convolution" | "CONVOLUTION") ~> "(" ~> rep1sep(factorVariable, ",") <~ ")" ^^ { varList =>
    ConvolutionFactorFunction(varList)
  }

  def samplingFactorFunction = ( "Average" | "Sampling" | "SAMPLING" | "SubSampling" ) ~> "(" ~> rep1sep(factorVariable, ",") <~ ")" ^^ { varList =>
    SamplingFactorFunction(varList)
  }

  def maxpoolingFactorFunction = ( "Maximum" | "maximum" | "max" | "maxpooling" | "Max" ) ~> "(" ~> rep1sep(factorVariable, ",") <~ ")" ^^ { varList =>
    MaxpoolingFactorFunction(varList)
  }

  def hiddenFactorFunction = ( "Hidden" ) ~> "(" ~> rep1sep(factorVariable, ",") <~ ")" ^^ { varList =>
    HiddenFactorFunction(varList)
  }

  def likelihoodFactorFunction = ("likelihood" | "Likelihood") ~> "(" ~> rep1sep(factorVariable, ",") <~ ")" ^^ { varList =>
    LikelihoodFactorFunction(varList)
  }

  def leastSquaresFactorFunction = ("leastSquares" | "LeastSquares" | "Squares" | "squares") ~> "(" ~> rep1sep(factorVariable, ",") <~ ")" ^^ { varList =>
    LeastSquaresFactorFunction(varList)
  }

 def softmaxFactorFunction = ("softmax" | "Softmax") ~> "(" ~> rep1sep(factorVariable, ",") <~ ")" ^^ { varList =>
    SoftmaxFactorFunction(varList)
  }

  def orFactorFunction = ("Or" | "OR") ~> "(" ~> rep1sep(factorVariable, ",") <~ ")" ^^ { varList =>
    OrFactorFunction(varList)
  }

  def xorFactorFunction = ("Xor" | "XOR") ~> "(" ~> rep1sep(factorVariable, ",") <~ ")" ^^ { varList =>
    XorFactorFunction(varList)
  }

  def andFactorFunction = ("And" | "AND") ~> "(" ~> rep1sep(factorVariable, ",") <~ ")" ^^ { varList =>
    AndFactorFunction(varList)
  }

  def equalFactorFunction = ("Equal" | "EQUAL") ~> "(" ~> factorVariable ~ ("," ~> factorVariable) <~ ")" ^^ { 
    case v1 ~ v2 =>
    EqualFactorFunction(List(v1, v2))
  }

  def isTrueFactorFunction = ("IsTrue" | "ISTRUE") ~> "(" ~> factorVariable <~ ")" ^^ { variable =>
    IsTrueFactorFunction(List(variable))
  }

  
  def factorVariable = ("!"?) ~ rep1sep(relationOrField, ".") ~ (arrayDefinition?) ~ 
    (("=" ~> equalPredicate)?) ^^ { 
    case (isNegated ~ varList ~ isArray ~ predicate)  => 
      FactorFunctionVariable(varList.take(varList.size - 1).mkString("."), varList.last, 
        isArray.isDefined, isNegated.isDefined, readLong(predicate))
  }

  def readLong(predicate: Option[String]) : Option[Long] = {
    predicate match {
      case Some(number) => Some(number.toLong)
      case None => None
    }
  }

  def factorFunc = implyFactorFunction | orFactorFunction | andFactorFunction | 
    equalFactorFunction | isTrueFactorFunction | xorFactorFunction | 
    convolutionFactorFunction | samplingFactorFunction | maxpoolingFactorFunction | hiddenFactorFunction |
    likelihoodFactorFunction | leastSquaresFactorFunction | 
    softmaxFactorFunction
}