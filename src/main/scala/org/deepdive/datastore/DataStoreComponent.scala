package org.deepdive.datastore

import org.deepdive.extraction.datastore._
import org.deepdive.inference._
import org.deepdive.calibration._

trait DataStoreComponent { 
  self: ExtractionDataStoreComponent with InferenceDataStoreComponent
  with CalibrationDataComponent =>
}

trait PostgresDataStoreComponent extends DataStoreComponent with 
  PostgresExtractionDataStoreComponent with PostgresInferenceDataStoreComponent with 
  PostgresCalibrationDataComponent