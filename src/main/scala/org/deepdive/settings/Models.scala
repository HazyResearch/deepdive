package org.deepdive.settings

/* Described the database connection */
case class Connection(host: String, port: Int, db: String, user: String, password: String)


/* A relation */
case class Relation(name: String, schema: Map[String, String], 
  foreignKeys: List[ForeignKey], evidenceField: Option[String])
case class ForeignKey(childRelation: String, childAttribute: String, parentRelation: String, 
  parentAttribute: String)


/* Extractors */
case class Extractor(name:String, outputRelation: String, inputQuery: String, udf: String, 
  factor: Option[FactorDesc])


/* Factor Description */
case class FactorDesc(name: String, func: FactorFunction, weight: FactorWeight)


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
  def variables : Seq[String]
}
case class ImplyFactorFunction(head: String, body: Seq[String]) extends FactorFunction {
  def variables = Seq(head) ++ body 
}


/* ETL Task */
case class EtlTask(relation: String, source: String)





