package org.deepdive.calibration

import org.deepdive.inference.InferenceDataStoreComponent

trait CalibrationData { self: InferenceDataStoreComponent =>


  /* Returns all relations and columns that correspond to variables */
  def relationsAndColumns(): Set[(String, String)]

  /*  The number of predictions in each bucket */
  def probabilityCounts(relation: String, column: String, buckets: List[Bucket]) : Map[Bucket, Long]
  
  /* The number of (correct, incorrect) predictions for each bucket */
  def predictionCounts(relation: String, column: String, buckets: List[Bucket]) : Map[Bucket, (Long, Long)]

}