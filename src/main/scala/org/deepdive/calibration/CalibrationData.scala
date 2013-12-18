package org.deepdive.calibration

import org.deepdive.Logging
import org.deepdive.inference.InferenceDataStoreComponent

trait CalibrationDataComponent { self: InferenceDataStoreComponent =>

  def calibrationData : CalibrationData

  trait CalibrationData extends Logging {
    
    /* Returns all relations and columns that correspond to variables */
    def relationsAndColumns(): Set[(String, String)]

    /*  The number of predictions in each bucket */
    def probabilityCounts(relation: String, column: String, buckets: List[Bucket]) : Map[Bucket, Long]
    
    /* The number of (correct, incorrect) predictions for each bucket */
    def predictionCounts(relation: String, column: String, buckets: List[Bucket]) : Map[Bucket, (Long, Long)]

    // Writes the counts for ten buckets from 0.0 to 1.0
    def writeBucketCounts(filePrefix: String) {
      val relationsAndColumns = calibrationData.relationsAndColumns()
      relationsAndColumns.foreach { case(relation, column) =>
        val file = new java.io.File(s"${filePrefix}_${relation}_${column}.tsv")
        file.getParentFile().mkdirs()
        log.info(s"Writing calibration data to file=${file.getCanonicalPath}")
        val writer = new java.io.PrintWriter(file)
        calibrationData.probabilityCounts(relation, column, Bucket.ten)
          .toList.sortBy(x => x._1.from).foreach { case(bucket, count) =>
          writer.println(f"${bucket.from}%2.2f\t${bucket.to}%2.2f\t${count}%d")
        }
        writer.close()
      }
    }

    def writeBucketPrecision(filePrefix: String) {
      val relationsAndColumns = calibrationData.relationsAndColumns()
      relationsAndColumns.foreach { case(relation, column) =>
        val file = new java.io.File(s"${filePrefix}_${relation}_${column}.tsv")
        file.getParentFile().mkdirs()
        log.info(s"Writing calibration data to file=${file.getCanonicalPath}")
        val writer = new java.io.PrintWriter(file)
        val sortedCounts = calibrationData.predictionCounts(relation, column, Bucket.ten).toList.sortBy(x => x._1.from)
        sortedCounts.foreach { case(bucket, Tuple2(countCorrect, countIncorrect)) =>
          writer.println(f"${bucket.from}%2.2f\t${bucket.to}%2.2f\t${countCorrect}%d\t${countIncorrect}%d")
        }
        writer.close()
      }
    }

  }

}