package org.deepdive.settings

/* Extractor specified in the settings */
case class Extractor(
  name: String, 
  style: String, 
  outputRelation: String, 
  inputQuery: InputQuery, 
  udf: String,
  parallelism: Int, 
  inputBatchSize: Int, 
  outputBatchSize: Int, 
  dependencies: Set[String],
  beforeScript: Option[String] = None, 
  afterScript: Option[String] = None, 
  sqlQuery: String = "",
  cmd: Option[String] = None,
  loader: String = "",
  loaderConfig: LoaderConfig = null
  )
