package org.deepdive.calibration

import akka.actor._
import scala.util.{Success, Failure}
import scala.language.postfixOps
import scala.sys.process._
import org.apache.commons.io.FilenameUtils
import org.deepdive.profiling.QuickReport
import org.deepdive.Context

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

      // Generate the calibration plot
      val deepDiveDir = Context.deepdiveHome
      val plotOutputFile = FilenameUtils.removeExtension(file.getCanonicalPath) + ".png"
      val calibrationCmd = Seq("gnuplot",
        "-e", s"""input_file='${file.getCanonicalPath}';output_file='${plotOutputFile}'""",
        s"${deepDiveDir}/util/calibration.plg")
      log.info(s"Running '${calibrationCmd}' to generate the calibration plot.")
      calibrationCmd! match {
        case 0 =>
          context.system.eventStream.publish(QuickReport("calibration", s"calibration plot written to ${plotOutputFile}"))
        case other =>
          val errorStr = s"ERROR generating calibration data plot. Run '${calibrationCmd}' manually."
          log.warning(errorStr)
          context.system.eventStream.publish(QuickReport("calibration", errorStr))
      }






      // Reply with success
      sender ! Success()
  }


}
