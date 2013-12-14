package org.deepdive.inference

import anorm._
import java.sql.Connection
import akka.event.Logging
import org.deepdive.context.Context
import java.io.{File, PrintWriter}

// Writes the factor Graph to CSV files. Ce's Format :)
object FileGraphWriter {

  val log = Logging.getLogger(Context.system, this)

  def dump(factorMapFile: File, factorsFile: File, 
    weightsFile: File)(implicit connection: Connection) {
    
    log.info(s"Writing weights to file=${weightsFile.getAbsolutePath}")
    writeWeights(weightsFile)

    log.info(s"Writing factors to file=${factorsFile.getAbsolutePath}")
    writeFactors(factorsFile)
    
    log.info(s"Writing factor_map to file=${factorMapFile.getAbsolutePath}")
    writeVariables(factorMapFile)
  
  
  }

  private def writeWeights(f: File)(implicit connection: Connection) {
    val writer = new PrintWriter(f)
    // [WEIGHT ID] [VALUE] [IS_FIXED_WEIGHT]
    // writer.println(List("weight_id", "initial_value", "is_fixed").mkString("\t"))
    SQL("select * from weights")().map { row => 
      List(
        row[Long]("id"), 
        row[Double]("initial_value"), 
        row[Boolean]("is_fixed")
      ).mkString("\t")
    }.foreach(writer.println(_))
    writer.close()
  }

  private def writeFactors(f: File)(implicit connection: Connection) {
    val writer = new PrintWriter(f)
    // [FACTOR_ID] [WEIGHT ID] [FACTOR_FUNC_TYPE] 
    // writer.println(List("factor_id", "weight_id", "factor_function").mkString("\t"))
    SQL("""select factors.*, count(factor_variables.variable_id) AS num_variables 
      FROM factors INNER JOIN factor_variables on factor_variables.factor_id = factors.id 
      GROUP BY factors.id, factors.weight_id""")().map { row => 
      List(
        row[Long]("id"),
        row[Long]("weight_id"),
        row[String]("factor_function")
      ).mkString("\t")
    }.foreach(writer.println(_))
    writer.close()
  }

  private def writeVariables(f: File)(implicit connection: Connection) {
    
    val writer = new PrintWriter(f)

    // [VARIABLE_ID] [FACTOR_ID] [POSITION] [IS_POSITIVE] [DATA_TYPE] [INITAL_VALUE] [IS_EVIDENCE] [IS_QUERY]
    // writer.println(List("variable_id", "factor_id", "position", "is_positive",
    //  "data_type", "initial_value", "is_evidence", "is_query").mkString("\t"))
    SQL("""SELECT variables.*, factor_variables.* 
      FROM variables LEFT JOIN factor_variables
      ON factor_variables.variable_id = variables.id""")().map{ row =>
      List(
        row[Long]("id"),
        row.get[Long]("factor_id").e.right.getOrElse(""),
        row.get[Long]("position").e.right.getOrElse(""),
        row.get[Boolean]("is_positive").e.right.getOrElse(""),
        row[String]("data_type"),
        row[Double]("initial_value"),
        row[Boolean]("is_evidence"),
        row[Boolean]("is_query")
      ).mkString("\t")
    }.foreach(writer.println(_))
    writer.close()
  }

}