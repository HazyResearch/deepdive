package org.deepdive.calibration

import anorm._
import org.deepdive.Logging
import org.deepdive.inference.PostgresInferenceDataStoreComponent

class PostgresCalibrationData extends CalibrationData with
  PostgresInferenceDataStoreComponent with Logging {

  implicit lazy val connection = inferenceDataStore.connection

  def relationsAndColumns() : Set[(String, String)] = {
    SQL("SELECT DISTINCT mapping_relation, mapping_column from variables;")().map { row =>
      Tuple2(row[String]("mapping_relation"), row[String]("mapping_column"))
    }.toSet
  }

  def probabilityCounts(relation: String, column: String, 
    buckets: List[Bucket]) : Map[Bucket, Long] = {
    val inferenceRelation = s"${relation}_${column}_inference"
    buckets.map { bucket =>
      val count = SQL(s"""SELECT COUNT(*) from ${inferenceRelation} 
        WHERE probability >= ${bucket.from} AND probability <= ${bucket.to};""")().head[Long]("count")
      (bucket, count)
    }.toMap
  }

  def predictionCounts(relation: String, column: String, 
    buckets: List[Bucket]) : Map[Bucket, (Long, Long)] = {
    val inferenceRelation = s"${relation}_${column}_inference"
    buckets.map { bucket =>
      val firstRow = SQL(s"""SELECT
        (SELECT COUNT(*) from ${inferenceRelation} 
        WHERE probability >= ${bucket.from} AND probability <= ${bucket.to} AND 
          ${column} IS NOT NULL AND last_sample = ${column}) 
        AS count_correct,
        (SELECT COUNT(*) from ${inferenceRelation} 
        WHERE probability >= ${bucket.from} AND probability <= ${bucket.to} AND last_sample IS NOT NULL 
        AND last_sample != ${column})
        AS count_incorrect;""")().head
      val numCorrect = firstRow[Long]("count_correct")
      val numIncorrect = firstRow[Long]("count_incorrect")
      (bucket, (numCorrect, numIncorrect))
    }.toMap
  }

}