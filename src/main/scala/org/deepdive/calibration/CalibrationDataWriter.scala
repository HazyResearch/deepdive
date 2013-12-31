package org.deepdive.calibration

import akka.actor._
import scala.util.{Success, Failure}

/* Compansion object for the CalibrationDataWriter */
object CalibrationDataWriter {
  def props = Props[CalibrationDataWriter]

  // Instructs the actor to write calibration data to a file
  case class WriteCalibrationData(fileName: String, data: Map[Bucket, BucketData])
}

class CalibrationDataWriter extends Actor with ActorLogging {

  override def preStart() { log.info("starting") }

  def receive = {
    case CalibrationDataWriter.WriteCalibrationData(fileName, data) =>
      // Create a new file and its parent directories
      val file = new java.io.File(fileName)
      file.getParentFile().mkdirs()
      // Write the data
      val writer = new java.io.PrintWriter(file)
      data.toList.sortBy(_._1.from).foreach { case(bucket, bucketData) =>
        writer.println(f"${bucket.from}%2.2f\t${bucket.to}%2.2f\t" +
          f"${bucketData.numVariables}%d\t${bucketData.numTrue}%d\t${bucketData.numFalse}%d")
      }
      writer.close()
      log.info(s"Wrote calibration_file=${file.getCanonicalPath}")
      sender ! Success()
  } 


}
