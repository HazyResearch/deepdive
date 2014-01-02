package org.deepdive.settings

case class InferenceSettings(factors: List[FactorDesc], insertBatchSize: Option[Int])