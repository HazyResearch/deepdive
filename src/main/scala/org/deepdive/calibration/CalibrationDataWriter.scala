package org.deepdive.calibration

import org.deepdive.Logging

class CalibrationDataWriter(data: CalibrationData) extends Logging {

  // Writes the counts for ten buckets from 0.0 to 1.0
  def writeBucketCounts(filePrefix: String) {
    val relationsAndColumns = data.relationsAndColumns()
    relationsAndColumns.foreach { case(relation, column) =>
      val file = new java.io.File(s"${filePrefix}_${relation}_${column}.tsv")
      file.getParentFile().mkdirs()
      log.info(s"Writing calibration data to file=${file.getCanonicalPath}")
      val writer = new java.io.PrintWriter(file)
      data.probabilityCounts(relation, column, Bucket.ten)
        .toList.sortBy(x => x._1.from).foreach { case(bucket, count) =>
        writer.println(f"${bucket.from}%2.2f\t${bucket.to}%2.2f\t${count}%d")
      }
      writer.close()
    }
  }

  def writeBucketPrecision(filePrefix: String) {
    val relationsAndColumns = data.relationsAndColumns()
    relationsAndColumns.foreach { case(relation, column) =>
      val file = new java.io.File(s"${filePrefix}_${relation}_${column}.tsv")
      file.getParentFile().mkdirs()
      log.info(s"Writing calibration data to file=${file.getCanonicalPath}")
      val writer = new java.io.PrintWriter(file)
      val sortedCounts = data.predictionCounts(relation, column, Bucket.ten).toList.sortBy(x => x._1.from)
      sortedCounts.foreach { case(bucket, Tuple2(countCorrect, countIncorrect)) =>
        writer.println(f"${bucket.from}%2.2f\t${bucket.to}%2.2f\t${countCorrect}%d\t${countIncorrect}%d")
      }
      writer.close()
    }
  }



}