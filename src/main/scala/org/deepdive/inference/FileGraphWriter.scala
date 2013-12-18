package org.deepdive.inference

import anorm._
import java.sql.Connection
import akka.event.Logging
import org.deepdive.context.Context
import java.io.{File, BufferedWriter, FileWriter, FileOutputStream}

/* Writes the factor graph to three TSV files for factors, weights and variables */
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
    val writer = new BufferedWriter(new FileWriter(f))
    // [WEIGHT ID] [VALUE] [IS_FIXED_WEIGHT]
    // writer.println(List("weight_id", "initial_value", "is_fixed").mkString("\t"))
    copySQLToTSV("""SELECT id, initial_value, 
      case when is_fixed then 'true' else 'false' end
      FROM weights""", f)
    writer.close()
  }

  private def writeFactors(f: File)(implicit connection: Connection) {
    copySQLToTSV("SELECT id, weight_id, factor_function FROM factors", f)
  }

  private def writeVariables(f: File)(implicit connection: Connection) {
    copySQLToTSV("""SELECT variables.id, factor_variables.factor_id, factor_variables.position,
      case when factor_variables.is_positive then 'true' else 'false' end, 
      variables.data_type, variables.initial_value, 
      case when variables.is_evidence then 'true' else 'false' end,
      case when variables.is_query then 'true' else 'false' end
      FROM variables LEFT JOIN factor_variables ON factor_variables.variable_id = variables.id""", f)
  }

  private def copySQLToTSV(sqlSelect: String, f: File)(implicit connection: Connection) {
    // We use Postgres' copy manager isntead of anorm to do efficient batch inserting
    // Do some magic to ge the underlying connection
    val del = new org.apache.commons.dbcp.DelegatingConnection(connection)
    val pg_conn = del.getInnermostDelegate().asInstanceOf[org.postgresql.core.BaseConnection]
    val cm = new org.postgresql.copy.CopyManager(pg_conn)
    val os = new FileOutputStream(f)
    val copySql = s"COPY ($sqlSelect) TO STDOUT"
    cm.copyOut(copySql, os)
    os.close()
  }

}