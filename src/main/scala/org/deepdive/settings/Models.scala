package org.deepdive.settings

/* Described the database connection */
case class Connection(host: String, port: Int, db: String, user: String, password: String)


/* A relation */
case class Relation(name: String, schema: Map[String, String], 
  foreignKeys: List[ForeignKey], queryField: Option[String])
case class ForeignKey(childRelation: String, childAttribute: String, parentRelation: String, 
  parentAttribute: String)


/* Extractors */
case class Extractor(name:String, outputRelation: String, inputQuery: String, udf: String)


/* Factor Description */
case class FactorDesc(name: String, relation: String, func: FactorFunction, weight: FactorWeight)


/* Factor Weights */
sealed trait FactorWeight {
  def variables : List[String]
}
case class KnownFactorWeight(value: Double) extends FactorWeight {
  def variables = Nil
}
case class UnknownFactorWeight(variables: List[String]) extends FactorWeight


/* Factor Functions */
sealed trait FactorFunction {
  def variables : Seq[FactorFunctionVariable]
  def newVariables : Seq[FactorFunctionVariable]
}
case class ImplyFactorFunction(head: FactorFunctionVariable, 
  body: Seq[FactorFunctionVariable]) extends FactorFunction {
  def variables = Seq(head) ++ body 
  def newVariables = Seq(head)
}

case class FactorFunctionVariable(foreignKey: Option[String], field: String)

object FactorFunctionVariable {
  implicit def stringToFactorFunctionVariable(str: String) : FactorFunctionVariable = {
    FactorFunctionParser.parse(FactorFunctionParser.factorVariable, str).get
  }
}


/* ETL Task */
case class EtlTask(relation: String, source: String)





