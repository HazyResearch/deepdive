package org.deepdive.settings

/* Extractor specified in the settings */
case class Extractor(name:String, outputRelation: String, inputQuery: InputQuery, udf: String, 
  parallelism: Int, inputBatchSize: Int, outputBatchSize: Int, dependencies: Set[String],
  beforeScript: Option[String]=None, afterScript: Option[String]=None)