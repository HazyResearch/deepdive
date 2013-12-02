package org.deepdive.extraction

case class ExtractionTask(name: String, outputRelation: String, 
  inputQuery: String, udf: String)