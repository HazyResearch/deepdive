package org.deepdive.inference

import anorm._
import java.sql.Connection
import akka.event.Logging
import org.deepdive.context.Context
import java.io.{File, PrintWriter}

// Writes the factor Graph to CSV files. Ce's Format :)
object FileGraphWriter {

  val log = Logging.getLogger(Context.system, this)

  def dump(variablesFile: File, factorsFile: File, 
    weightsFile: File)(implicit connection: Connection) {
    
    log.debug(s"Writing weights to file=${weightsFile.getAbsolutePath}")
    writeWeights(weightsFile)
    
    log.debug(s"Writing variables to file=${variablesFile.getAbsolutePath}")
    writeVariables(variablesFile)
    
    log.debug(s"Writing factors to file=${factorsFile.getAbsolutePath}")
    writeFactors(factorsFile)
  
  }

  private def writeFactors(f: File)(implicit connection: Connection) {
    val writer = new PrintWriter(f)
    // [FACTOR_ID] [NUM_VARIABLE_IN_FACTOR] [FACTOR_FUN_ID] [WEIGHT ID]
    SQL("""select factors.*, count(factor_variables.variable_id) AS num_variables 
      FROM factors INNER JOIN factor_variables on factor_variables.factor_id = factors.id 
      GROUP BY factors.id""")().map { row => 
      List(
        row[Long]("id"), 
        row[Long]("num_variables"),
        row[Long]("factor_function_id"), 
        row[Long]("weight_id")
      ).mkString("\t")
    }.foreach(writer.println(_))
    writer.close()
  }

  private def writeWeights(f: File)(implicit connection: Connection) {
    val writer = new PrintWriter(f)
    // [WEIGHT ID] [VALUE] [IS_FIXED_WEIGHT]
    SQL("select * from weights")().map { row => 
      List(row[Long]("id"), row[Double]("value"), row[Boolean]("is_fixed").compare(false)).mkString("\t")
    }.foreach(writer.println(_))
    writer.close()
  }

  private def writeVariables(f: File)(implicit connection: Connection) {
    
    // Support Anorm Sequence conversion
    implicit def rowToSeq[T]: Column[Seq[T]] = Column.nonNull { (value, meta) =>
      val MetaDataItem(qualified, nullable, clazz) = meta
      value match {
        case arr: java.sql.Array => Right(arr.getArray.asInstanceOf[Array[_]].map(_.asInstanceOf[T]).toSeq)
        case _ => Left(TypeDoesNotMatch(
          "Cannot convert " + value + ":" + 
          value.asInstanceOf[AnyRef].getClass + " to Seq[String] for column " + qualified))
      } 
    }

    val writer = new PrintWriter(f)

    // [VARIABLE_ID] [PROPERTY] [LOWER_BOUND] [UPPER_BOUND] [NUMBER_OF_FACTOR] [FACTOR_INFO …] [INIT VALUE]
    SQL("""SELECT variables.*, array_agg(factor_variables.factor_id) AS factors,
        array_agg(factor_variables.position) AS positions, array_agg(factor_variables.is_positive) AS signs
      FROM variables INNER JOIN factor_variables ON variables.id = factor_variables.variable_id
      GROUP BY variables.id""")().map{ row =>
      
      val factors = row[Seq[Long]]("factors")
      val positions = row[Seq[Int]]("positions")
      val signs = row[Seq[Boolean]]("signs")
      val factorInfo = (factors, positions, signs).zipped.map { case(x,y,z) =>
        // Note: We set GROUO ID to 0
        s"$x\t0\t$y\t${z.compare(false)}"
      }.mkString("\t")
      List(
        row[Long]("id"),
        row[String]("variable_type"),
        row[Double]("lower_bound"),
        row[Double]("upper_bound"),
        factors.size,
        factorInfo,
        row[Double]("initial_value")
      ).mkString("\t")
    }.foreach(writer.println(_))
    writer.close()
  }

}