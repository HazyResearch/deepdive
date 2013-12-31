package org.deepdive.datastore

import org.deepdive.extraction.datastore._
import org.deepdive.inference._
import org.deepdive.calibration._

trait DataStoreComponent { 
  this: ExtractionDataStoreComponent with InferenceDataStoreComponent =>
}

trait PostgresDataStoreComponent extends DataStoreComponent with 
  PostgresExtractionDataStoreComponent with PostgresInferenceDataStoreComponent