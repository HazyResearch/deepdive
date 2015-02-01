package org.deepdive.inference

import anorm._
import au.com.bytecode.opencsv.CSVWriter
import java.io.{ByteArrayInputStream, File, FileOutputStream, FileWriter,
  StringWriter, Reader, FileReader, InputStream, InputStreamReader}
import org.deepdive.calibration._
import org.deepdive.datastore.PostgresDataStore
import org.deepdive.Logging
import org.deepdive.settings._
import org.deepdive.helpers.Helpers
import scala.collection.mutable.{ArrayBuffer, Set, SynchronizedBuffer}
import scala.io.Source
import java.io._

/* Stores the factor graph and inference results in a postges database. */
trait PostgresInferenceDataStoreComponent extends SQLInferenceDataStoreComponent {

  class PostgresInferenceDataStore(val dbSettings : DbSettings) extends SQLInferenceDataStore with Logging {

    def ds = PostgresDataStore

    // Default batch size, if not overwritten by user
    val BatchSize = Some(250000)
      

    /**
     * weightsFile: location to the binary format. Assume "weightsFile.text" file exists.
     */
    def bulkCopyWeights(weightsFile: String, dbSettings: DbSettings) : Unit = {
    
     val cmdfile = File.createTempFile(s"copy", ".sh")
     val writer = new PrintWriter(cmdfile)
     val copyStr = List("psql ", Helpers.getOptionString(dbSettings), " -c ", "\"", 
       """\COPY """, s"${WeightResultTable}(id, weight) FROM \'${weightsFile}\' DELIMITER ' ';", "\"").mkString("")
     log.info(copyStr)
     writer.println(copyStr)
     writer.close()
     Helpers.executeCmd(cmdfile.getAbsolutePath())
    }
    
    def bulkCopyVariables(variablesFile: String, dbSettings: DbSettings) : Unit = {
     
     val cmdfile = File.createTempFile(s"copy", ".sh")
     val writer = new PrintWriter(cmdfile)
     val copyStr = List("psql ", Helpers.getOptionString(dbSettings), " -c ", "\"", 
       """\COPY """, s"${VariableResultTable}(id, category, expectation) FROM \'${variablesFile}\' DELIMITER ' ';", "\"").mkString("")
     log.info(copyStr)
     writer.println(copyStr)
     writer.close()
     Helpers.executeCmd(cmdfile.getAbsolutePath())
   }

    /**
     * Drop and create a sequence, based on database type.
     */
    def createSequenceFunction(seqName: String): String =
      s"""DROP SEQUENCE IF EXISTS ${seqName} CASCADE;
          CREATE SEQUENCE ${seqName} MINVALUE -1 START 0;"""

    /**
     * Get the next value of a sequence
     */
    def nextVal(seqName: String): String =
      s""" nextval('${seqName}') """

    /**
     * Cast an expression to a type
     */
    def cast(expr: Any, toType: String): String =
      s"""${expr.toString()}::${toType}"""

    /**
     * Given a string column name, Get a quoted version dependent on DB.
     *          if psql, return "column"
     *          if mysql, return `column`
     */
    def quoteColumn(column: String): String =
      '"' + column + '"'
      
    /**
     * Generate random number in [0,1] in psql
     */
    def randomFunction: String = "RANDOM()"

    /**
     * Concatinate strings using "||" in psql/GP, adding user-specified
     * delimiter in between
     */
    def concat(list: Seq[String], delimiter: String): String = {
      delimiter match {
        case null => list.mkString(s" || ")
        case "" => list.mkString(s" || ")
        case _ => list.mkString(s" || '${delimiter}' || ")
      }
    }

    /**
     * For postgres, do not create indexes for query table
     */
    override def createIndexesForQueryTable(queryTable: String, weightVariables: Seq[String]) = {
      // do nothing
    }

    // check whether greenplum is used
    def isUsingGreenplum() : Boolean = {
      var usingGreenplum = false
      ds.executeSqlQueryWithCallback("""SELECT version() LIKE '%Greenplum%';""") { rs => 
        usingGreenplum = rs.getBoolean(1) 
      }
      return usingGreenplum
    }
    
    // create fast sequence assign function for greenplum
    def createAssignIdFunctionGreenplum() : Unit = {
      if (!isUsingGreenplum()) return
      val sql = """
      DROP LANGUAGE IF EXISTS plpgsql CASCADE;
      DROP LANGUAGE IF EXISTS plpythonu CASCADE;
      CREATE LANGUAGE plpgsql;
      CREATE LANGUAGE plpythonu;
  
      CREATE OR REPLACE FUNCTION clear_count_1(sid int) RETURNS int AS 
      $$
      if '__count_1' in SD:
        SD['__count_1'] = -1
        return 1
      return 0
      $$ LANGUAGE plpythonu;
       
       
      CREATE OR REPLACE FUNCTION updateid(startid bigint, sid int, sids int[], base_ids bigint[], base_ids_noagg bigint[]) RETURNS bigint AS 
      $$
      if '__count_1' in SD:
        a = SD['__count_2']
        b = SD['__count_1']
        SD['__count_2'] = SD['__count_2'] - 1
        if SD['__count_2'] < 0:
          SD.pop('__count_1')
        return startid+b-a
      else:
        for i in range(0, len(sids)):
          if sids[i] == sid:
            SD['__count_1'] = base_ids[i] - 1
            SD['__count_2'] = base_ids_noagg[i] - 1
        a = SD['__count_2']
        b = SD['__count_1']
        SD['__count_2'] = SD['__count_2'] - 1
        if SD['__count_2'] < 0:
          SD.pop('__count_1')
        return startid+b-a
        
      $$ LANGUAGE plpythonu;
       
      CREATE OR REPLACE FUNCTION fast_seqassign(tname character varying, startid bigint) RETURNS TEXT AS 
      $$
      BEGIN
        EXECUTE 'drop table if exists tmp_gpsid_count cascade;';
        EXECUTE 'drop table if exists tmp_gpsid_count_noagg cascade;';
        EXECUTE 'create table tmp_gpsid_count as select gp_segment_id as sid, count(clear_count_1(gp_segment_id)) as base_id from ' || quote_ident(tname) || ' group by gp_segment_id order by sid distributed by (sid);';
        EXECUTE 'create table tmp_gpsid_count_noagg as select * from tmp_gpsid_count distributed by (sid);';
        EXECUTE 'update tmp_gpsid_count as t set base_id = (SELECT SUM(base_id) FROM tmp_gpsid_count as t2 WHERE t2.sid <= t.sid);';
        RAISE NOTICE 'EXECUTING _fast_seqassign()...';
        EXECUTE 'select * from _fast_seqassign(''' || quote_ident(tname) || ''', ' || startid || ');';
        RETURN '';
      END;
      $$ LANGUAGE 'plpgsql';
       
      CREATE OR REPLACE FUNCTION _fast_seqassign(tname character varying, startid bigint)
      RETURNS TEXT AS
      $$
      DECLARE
        sids int[] :=  ARRAY(SELECT sid FROM tmp_gpsid_count ORDER BY sid);
        base_ids bigint[] :=  ARRAY(SELECT base_id FROM tmp_gpsid_count ORDER BY sid);
        base_ids_noagg bigint[] :=  ARRAY(SELECT base_id FROM tmp_gpsid_count_noagg ORDER BY sid);
        tsids text;
        tbase_ids text;
        tbase_ids_noagg text;
      BEGIN
        SELECT INTO tsids array_to_string(sids, ',');
        SELECT INTO tbase_ids array_to_string(base_ids, ',');
        SELECT INTO tbase_ids_noagg array_to_string(base_ids_noagg, ',');
        if ('update ' || tname || ' set id = updateid(' || startid || ', gp_segment_id, ARRAY[' || tsids || '], ARRAY[' || tbase_ids || '], ARRAY[' || tbase_ids_noagg || ']);')::text is not null then
          EXECUTE 'update ' || tname || ' set id = updateid(' || startid || ', gp_segment_id, ARRAY[' || tsids || '], ARRAY[' || tbase_ids || '], ARRAY[' || tbase_ids_noagg || ']);';
        end if;
        RETURN '';
      END;
      $$
      LANGUAGE 'plpgsql';
      """
      ds.executeSqlQuery(sql)
    }

  }
}
