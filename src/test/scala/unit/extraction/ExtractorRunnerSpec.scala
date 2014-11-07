package org.deepdive.test.unit

import java.io._
import org.deepdive.Logging
import com.typesafe.config._
import akka.actor._
import akka.testkit._
import org.deepdive.datastore._
import org.deepdive.extraction._
import org.deepdive.extraction.datastore._
import org.deepdive.extraction.ProcessExecutor._
import org.deepdive.settings._
import org.deepdive.helpers.Helpers
import org.deepdive.test.helpers._
import org.scalatest._
import scala.concurrent.duration._
import play.api.libs.json._
import scalikejdbc._

class ExtractorRunnerSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with FunSpecLike with BeforeAndAfter with Logging {

  val dataStore = TestHelper.getTestEnv match {
    case TestHelper.Psql => new PostgresExtractionDataStore
    case TestHelper.Mysql=> new MysqlExtractionDataStore
  }
  
  // execute a query
  def execute(ds : JdbcDataStore, sql: String) = {
    log.debug("EXECUTING.... " + sql)
    val conn = ds.borrowConnection()
    val stmt = conn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
      java.sql.ResultSet.CONCUR_UPDATABLE)
    try {
      """;\s+""".r.split(sql.trim()).filterNot(_.isEmpty).foreach(q => 
        conn.prepareStatement(q.trim()).executeUpdate)
    } catch {
      // SQL cmd exception
      case exception : Throwable =>
      log.error(exception.toString)
      throw exception
    } finally {
      conn.close()
    }
    log.debug("DONE!")
  }

  def writeToFile(p: File, s: String): Unit = {
    log.debug(s"Writing to file ${p.getAbsolutePath()}:\n${s}")
    val pw = new java.io.PrintWriter(p)
    try pw.write(s) finally pw.close()
  }

  var inited = false

  val config = ConfigFactory.parseString(TestHelper.getConfig).withFallback(ConfigFactory.load)

  before {
    if(!inited){
      JdbcDataStore.init(config)
      inited = true
    }
    dataStore.init()

    TestHelper.getTestEnv match {
      case TestHelper.Psql =>
        dataStore.ds.DB.autoCommit { implicit session =>
          // TODO side effect?
          SQL("drop schema if exists public cascade; create schema public;").execute.apply()
          SQL("""DROP TABLE IF EXISTS relation1 CASCADE;""").execute.apply()
          SQL("""CREATE TABLE relation1(id bigint, key int);""").execute.apply()
          SQL("""DROP TABLE IF EXISTS testtable CASCADE;""").execute.apply()
        }

      case TestHelper.Mysql =>
        dataStore.ds.withConnection { implicit conn =>
          SQL("""DROP TABLE IF EXISTS relation1 CASCADE;""").execute()
          SQL("""CREATE TABLE relation1(id bigint, key int);""").execute()
          SQL("""DROP TABLE IF EXISTS testtable CASCADE;""").execute()
        }
    }
  } 

  after {
    //JdbcDataStore.close()
  }

  def this() = this(ActorSystem("ExtractorRunnerSpec"))

  val dbSettings = TestHelper.getDbSettings

  val sqlScriptPrefix = TestHelper.getTestEnv match {
    case TestHelper.Psql => s"psql ${Helpers.getOptionString(dbSettings)} -c "
    case TestHelper.Mysql=> s"mysql ${Helpers.getOptionString(dbSettings)} -e "
  }
  // lazy implicit val session = ds.DB.autoCommitSession()

  describe("Running extractor-type-independent task (e.g., before/after script)"){
    
    it("should work for before script"){
      execute(dataStore.ds, "drop table if exists testtable;")
      execute(dataStore.ds, "create table testtable ( a text );")
      val t = java.io.File.createTempFile("test", ".sh")
      val t2 = java.io.File.createTempFile("test", ".tsv")
      val t3 = java.io.File.createTempFile("test", ".py")

      log.info(t.getAbsolutePath)

      writeToFile(t, s"""
        | echo "I should be in the table" > ${t2.getAbsolutePath} 
        | echo "I should also be in the table" >> ${t2.getAbsolutePath}
      """.stripMargin)

      writeToFile(t3, 
     s"""|#! /usr/bin/python
         |import json
         |for l in open('${t2.getAbsolutePath}'):
         |  print json.dumps({'a':l.strip()})
         |
      |""".stripMargin)

      val actor = system.actorOf(ExtractorRunner.props(dataStore, dbSettings))

      // TODO do not run json_extractor for MySQL
      assume(TestHelper.getTestEnv != TestHelper.Mysql)


      val task = new ExtractionTask(Extractor("testExtractor", "json_extractor", "testtable", 
        "SELECT 5", t3.getAbsolutePath, 1, 1000, 1000, Nil.toSet, Some(t.getAbsolutePath), None, "", None))
      actor ! ExtractorRunner.SetTask(task)
      watch(actor)
      //expectMsg("Done!")
      //expectTerminated(actor)
      expectMsgAnyClassOf(classOf[String], classOf[Terminated])
      expectMsgAnyClassOf(classOf[String], classOf[Terminated])


      dataStore.ds.DB.readOnly { implicit session =>
        val numRecords = SQL(s"""SELECT COUNT(*) AS "count" FROM testtable;""")
          .map(rs => rs.long("count")).single.apply().get
        assert(numRecords === 2)
      }

      execute(dataStore.ds, "DELETE FROM testtable WHERE a='I should be in the table';");
      execute(dataStore.ds, "DELETE FROM testtable WHERE a='I should also be in the table';");

      dataStore.ds.DB.readOnly { implicit session =>
        val numRecords = SQL(s"""SELECT COUNT(*) AS "count" FROM testtable;""")
          .map(rs => rs.long("count")).single.apply().get
        assert(numRecords === 0)
      }
    }
    
    it("should fail if before script is not executable"){

      val t = java.io.File.createTempFile("test", ".sh")
      val t2 = java.io.File.createTempFile("test", ".tsv")
      val t3 = java.io.File.createTempFile("test", ".py")

      // TODO do not run json_extractor for MySQL
      assume(TestHelper.getTestEnv != TestHelper.Mysql)


      val actor = system.actorOf(ExtractorRunner.props(dataStore, dbSettings))
      val task = new ExtractionTask(Extractor("testExtractor", "json_extractor", "testtable", 
        "SELECT 5", t3.getAbsolutePath, 1, 1000, 1000, Nil.toSet, Some("/bin/i_am_not_exist"), None, "", None))

      actor ! ExtractorRunner.SetTask(task)
      watch(actor)
      expectMsgAnyClassOf(classOf[Status.Failure], classOf[Terminated])
      expectMsgAnyClassOf(classOf[Status.Failure], classOf[Terminated])

    }
    
    it("should fail if before script is executable but contains errors"){

      // TODO do not run json_extractor for MySQL
      assume(TestHelper.getTestEnv != TestHelper.Mysql)
      execute(dataStore.ds, "drop table if exists testtable;")
      execute(dataStore.ds, "create table testtable ( a text );")
      val t = java.io.File.createTempFile("test", ".py")
      val t2 = java.io.File.createTempFile("test", ".tsv")
      val t3 = java.io.File.createTempFile("test", ".py")

      log.info(t.getAbsolutePath)

      writeToFile(t, 
      s"""|#! /usr/bin/python
          | echo "I should also be in the table" >> ${t2.getAbsolutePath}
      """.stripMargin)

      writeToFile(t3, 
     s"""|#! /usr/bin/python
         |import json
         |for l in open('${t2.getAbsolutePath}'):
         |  print json.dumps({'a':l.strip()})
         |
      |""".stripMargin)

      val actor = system.actorOf(ExtractorRunner.props(dataStore, dbSettings))

      val task = new ExtractionTask(Extractor("testExtractor", "json_extractor", "testtable", 
        "SELECT 5", t3.getAbsolutePath, 1, 1000, 1000, Nil.toSet, Some(t.getAbsolutePath), None, "", None))
      actor ! ExtractorRunner.SetTask(task)
      watch(actor)
      expectMsgAnyClassOf(classOf[Status.Failure], classOf[Terminated])
      expectMsgAnyClassOf(classOf[Status.Failure], classOf[Terminated])

    }

    it("should work for after script"){

      // TODO do not run json_extractor for MySQL
      assume(TestHelper.getTestEnv != TestHelper.Mysql)
      execute(dataStore.ds, "drop table if exists testtable;")
      execute(dataStore.ds, "create table testtable ( a text );")
      val t = java.io.File.createTempFile("test", ".sh")
      val t2 = java.io.File.createTempFile("test", ".tsv")
      val t3 = java.io.File.createTempFile("test", ".py")
      val t4 = java.io.File.createTempFile("test", ".sh")

      log.info(t.getAbsolutePath)

      writeToFile(t, s"""
        | echo "I should be in the table" > ${t2.getAbsolutePath} 
        | echo "I should also be in the table" >> ${t2.getAbsolutePath}
      """.stripMargin)

      writeToFile(t3, 
     s"""|#! /usr/bin/python
         |import json
         |for l in open('${t2.getAbsolutePath}'):
         |  print json.dumps({'a':l.strip()})
         |
      |""".stripMargin)


      writeToFile(t4, s"""
        | ${sqlScriptPrefix} "INSERT INTO testtable VALUES ('Hello!');"
        | ${sqlScriptPrefix} "INSERT INTO testtable VALUES('Aloha!');"
      """.stripMargin)

      val actor = system.actorOf(ExtractorRunner.props(dataStore, dbSettings))

      val task = new ExtractionTask(Extractor("testExtractor", "json_extractor", "testtable", 
        "SELECT 5", t3.getAbsolutePath, 1, 1000, 1000, Nil.toSet, Some(t.getAbsolutePath), Some(t4.getAbsolutePath), "", None))
      actor ! ExtractorRunner.SetTask(task)
      watch(actor)
      //expectMsg("Done!")
      //expectTerminated(actor)
      expectMsgAnyClassOf(classOf[String], classOf[Terminated])
      expectMsgAnyClassOf(classOf[String], classOf[Terminated])

      dataStore.ds.DB.readOnly { implicit session =>
        val numRecords = SQL(s"""SELECT COUNT(*) AS "count" FROM testtable;""")
          .map(rs => rs.long("count")).single.apply().get
        assert(numRecords === 4)
      }

      execute(dataStore.ds, "DELETE FROM testtable WHERE a='I should be in the table';");
      execute(dataStore.ds, "DELETE FROM testtable WHERE a='I should also be in the table';");
      execute(dataStore.ds, "DELETE FROM testtable WHERE a='Hello!';");
      execute(dataStore.ds, "DELETE FROM testtable WHERE a='Aloha!';");

      dataStore.ds.DB.readOnly { implicit session =>
        val numRecords = SQL(s"""SELECT COUNT(*) AS "count" FROM testtable;""")
          .map(rs => rs.long("count")).single.apply().get
        assert(numRecords === 0)
      }

    }
    
    it("should fail if after script is not executable"){

      // TODO do not run json_extractor for MySQL
      assume(TestHelper.getTestEnv != TestHelper.Mysql)
      execute(dataStore.ds, "drop table if exists testtable;")
      execute(dataStore.ds, "create table testtable ( a text );")

      val t = java.io.File.createTempFile("test", ".sh")
      val t2 = java.io.File.createTempFile("test", ".tsv")
      val t3 = java.io.File.createTempFile("test", ".py")

      val actor = system.actorOf(ExtractorRunner.props(dataStore, dbSettings))
      val task = new ExtractionTask(Extractor("testExtractor", "json_extractor", "testtable", 
        "SELECT 5", t3.getAbsolutePath, 1, 1000, 1000, Nil.toSet, None, Some("/bin/i_am_not_exist"), "", None))

      actor ! ExtractorRunner.SetTask(task)
      watch(actor)
      expectMsgAnyClassOf(classOf[Status.Failure], classOf[Terminated])
      expectMsgAnyClassOf(classOf[Status.Failure], classOf[Terminated])

    }
    
    it("should fail if after script is executable but contains errors"){

      // TODO do not run json_extractor for MySQL
      assume(TestHelper.getTestEnv != TestHelper.Mysql)
      execute(dataStore.ds, "drop table if exists testtable;")
      execute(dataStore.ds, "create table testtable ( a text );")
      val t = java.io.File.createTempFile("test", ".py")
      val t2 = java.io.File.createTempFile("test", ".tsv")
      val t3 = java.io.File.createTempFile("test", ".py")

      log.info(t.getAbsolutePath)

      writeToFile(t, 
      s"""|#! /usr/bin/python
          | echo "I should also be in the table" >> ${t2.getAbsolutePath}
      """.stripMargin)

      writeToFile(t3, 
     s"""|#! /usr/bin/python
         |import json
         |for l in open('${t2.getAbsolutePath}'):
         |  print json.dumps({'a':l.strip()})
         |
      |""".stripMargin)

      val actor = system.actorOf(ExtractorRunner.props(dataStore, dbSettings))

      val task = new ExtractionTask(Extractor("testExtractor", "json_extractor", "testtable", 
        "SELECT 5", t3.getAbsolutePath, 1, 1000, 1000, Nil.toSet, None, Some(t.getAbsolutePath), "", None))
      actor ! ExtractorRunner.SetTask(task)
      watch(actor)
      expectMsgAnyClassOf(classOf[Status.Failure], classOf[Terminated])
      expectMsgAnyClassOf(classOf[Status.Failure], classOf[Terminated])
    }
  }

  describe("Running an json_extractor extraction task") {

    it("should work without parallelism") {
      // TODO do not run json_extractor for MySQL
      assume(TestHelper.getTestEnv != TestHelper.Mysql)

      val actor = system.actorOf(ExtractorRunner.props(dataStore, dbSettings))
      dataStore.addBatch(List(Json.parse("""{"key": 5}""").asInstanceOf[JsObject]).iterator, "relation1")

      dataStore.ds.DB.readOnly { implicit session =>
        val numRecords = SQL(s"""SELECT COUNT(*) AS "count" FROM relation1""")
          .map(rs => rs.long("count")).single.apply().get
        assert(numRecords === 1)
      }

      val task = new ExtractionTask(Extractor("testExtractor", "json_extractor", "relation1", 
        "SELECT * FROM relation1", "/bin/cat", 1, 1000, 1000, Nil.toSet, None, None, "SELECT * FROM relation1", None))
      actor ! ExtractorRunner.SetTask(task)
      watch(actor)
      //expectMsg("Done!")
      //expectTerminated(actor)
      expectMsgAnyClassOf(classOf[String], classOf[Terminated])
      expectMsgAnyClassOf(classOf[String], classOf[Terminated])

      dataStore.ds.DB.readOnly { implicit session =>
        val numRecords = SQL(s"""SELECT COUNT(*) AS "count" FROM relation1""")
          .map(rs => rs.long("count")).single.apply().get
        assert(numRecords === 2)
      }
    }

    it("should work when the input is empty") {
      // TODO do not run json_extractor for MySQL
      assume(TestHelper.getTestEnv != TestHelper.Mysql)

      val actor = system.actorOf(ExtractorRunner.props(dataStore, dbSettings))
      val task = new ExtractionTask(Extractor("testExtractor", "json_extractor", "relation1", 
        "SELECT * FROM relation1", "/bin/cat", 1, 1000, 1000, Nil.toSet))
      actor ! ExtractorRunner.SetTask(task)
      watch(actor)
      //expectMsg("Done!")
      //expectTerminated(actor)
      expectMsgAnyClassOf(classOf[String], classOf[Terminated])
      expectMsgAnyClassOf(classOf[String], classOf[Terminated])

      dataStore.ds.DB.readOnly { implicit session =>
        val numRecords = SQL(s"""SELECT COUNT(*) AS "count" FROM relation1""")
          .map(rs => rs.long("count")).single.apply().get
        assert(numRecords === 0)
      }
    }

    it("should work with parallelism") {
      // TODO do not run json_extractor for MySQL
      assume(TestHelper.getTestEnv != TestHelper.Mysql)


      // TODO do not run json_extractor for MySQL
      assume(TestHelper.getTestEnv != TestHelper.Mysql)


      val actor = system.actorOf(ExtractorRunner.props(dataStore, dbSettings))
      val batchData = (1 to 1000).map { i =>
        Json.parse(s"""{"key": ${i}}""").asInstanceOf[JsObject]
      }.toList
      dataStore.addBatch(batchData.iterator, "relation1")
      val task = new ExtractionTask(Extractor("testExtractor", "json_extractor", "relation1", 
        "SELECT * FROM relation1", "/bin/cat", 4, 500, 200, Nil.toSet, None, None, "", None))
      actor ! ExtractorRunner.SetTask(task)
      watch(actor)
      //expectMsg("Done!")
      //expectTerminated(actor)
      expectMsgAnyClassOf(classOf[String], classOf[Terminated])
      expectMsgAnyClassOf(classOf[String], classOf[Terminated])
      dataStore.ds.DB.readOnly { implicit session =>
        val numRecords = SQL(s"""SELECT COUNT(*) AS "count" FROM relation1""")
          .map(rs => rs.long("count")).single.apply().get
        assert(numRecords === 2000)
      }
    }

    it("should return failure when the task failes") {
      // TODO do not run json_extractor for MySQL
      assume(TestHelper.getTestEnv != TestHelper.Mysql)

      val actor = system.actorOf(ExtractorRunner.props(dataStore, dbSettings))
      val failingExtractorFile = getClass.getResource("/failing_extractor.py").getFile
      dataStore.addBatch(List(Json.parse("""{"key": 5}""").asInstanceOf[JsObject]).iterator, "relation1")
      val task = new ExtractionTask(Extractor("testExtractor", "json_extractor", "relation1", 
        "SELECT * FROM relation1", failingExtractorFile, 1, 1000, 1000, Nil.toSet, None, None, "", None))
      actor ! ExtractorRunner.SetTask(task)
      watch(actor)
      expectMsgAnyClassOf(classOf[Status.Failure], classOf[Terminated])
      expectMsgAnyClassOf(classOf[Status.Failure], classOf[Terminated])
    }

    it("should correctly execute the before and after scripts") {
      // TODO do not run json_extractor for MySQL
      assume(TestHelper.getTestEnv != TestHelper.Mysql)

      val actor = system.actorOf(ExtractorRunner.props(dataStore, dbSettings))
      val task = new ExtractionTask(Extractor("testExtractor", "json_extractor", "relation1", 
        "SELECT * FROM relation1", "/bin/cat", 1, 1000, 1000, Nil.toSet, Option("echo Hello"), Option("echo World"), "", None))
      actor ! ExtractorRunner.SetTask(task)
      watch(actor)
      //expectMsg("Done!")
      //expectTerminated(actor)
      expectMsgAnyClassOf(classOf[String], classOf[Terminated])
      expectMsgAnyClassOf(classOf[String], classOf[Terminated])
    }

    it("should return a failure when the query is invalid") {
      // TODO do not run json_extractor for MySQL
      assume(TestHelper.getTestEnv != TestHelper.Mysql)

      val actor = system.actorOf(ExtractorRunner.props(dataStore, dbSettings))
      val task = new ExtractionTask(Extractor("testExtractor", "json_extractor", "relation5", 
        "relation1", "/bin/cat", 1, 1000, 1000, Nil.toSet,None, None, "", None))
      actor ! ExtractorRunner.SetTask(task)
      watch(actor)
      expectMsgAnyClassOf(classOf[Status.Failure], classOf[Terminated])
      expectMsgAnyClassOf(classOf[Status.Failure], classOf[Terminated])
    }

    it("should return a failure when the before script crashes") {
      // TODO do not run json_extractor for MySQL
      assume(TestHelper.getTestEnv != TestHelper.Mysql)

      val actor = system.actorOf(ExtractorRunner.props(dataStore, dbSettings))
      val task = new ExtractionTask(Extractor("testExtractor", "json_extractor", "relation1", 
        "SELECT * FROM relation1", "/bin/cat", 1, 1000, 1000, Nil.toSet, Option("/bin/OHNO!"), Option("echo World"), "", None))
      actor ! ExtractorRunner.SetTask(task)
      watch(actor)
      expectMsgAnyClassOf(classOf[Status.Failure], classOf[Terminated])
      expectMsgAnyClassOf(classOf[Status.Failure], classOf[Terminated])
    }

    it("should return a failure when the after script crashes") {
      // TODO do not run json_extractor for MySQL
      assume(TestHelper.getTestEnv != TestHelper.Mysql)

      val actor = system.actorOf(ExtractorRunner.props(dataStore, dbSettings))
      val task = new ExtractionTask(Extractor("testExtractor", "json_extractor", "relation1", 
        "SELECT * FROM relation1", "/bin/cat", 1, 1000, 1000, Nil.toSet, Option("echo Hello"), Option("/bin/OHNO!"), "", None))
      actor ! ExtractorRunner.SetTask(task)
      watch(actor)
      expectMsgAnyClassOf(classOf[Status.Failure], classOf[Terminated])
      expectMsgAnyClassOf(classOf[Status.Failure], classOf[Terminated])
    }

  }

  describe("Running an TSV extractor"){

    it("should work for trivial TSV extractor with one single column output and trivial input (not from SQL)"){

      execute(dataStore.ds, "drop table if exists testtable ;")
      execute(dataStore.ds, "create table testtable ( a text );")
      val t = java.io.File.createTempFile("test", ".sh")
      val t2 = java.io.File.createTempFile("test", ".tsv")
      val t3 = java.io.File.createTempFile("test", ".py")
      t3.setExecutable(true, false)
      t2.setExecutable(true, false)

      log.info(t.getAbsolutePath)

      writeToFile(t, s"""
        | echo "I should be in the table" > ${t2.getAbsolutePath} 
        | echo "I should also be in the table" >> ${t2.getAbsolutePath}
      """.stripMargin)

      writeToFile(t3, 
     s"""|#! /usr/bin/python
         |import json
         |for l in open('${t2.getAbsolutePath}'):
         |  print l.strip()
         |
      |""".stripMargin)

      val actor = system.actorOf(ExtractorRunner.props(dataStore, dbSettings))

      val task = new ExtractionTask(Extractor("testExtractor", "tsv_extractor", "testtable", 
        "SELECT 5", t3.getAbsolutePath, 1, 1000, 1000, Nil.toSet, Some(t.getAbsolutePath), None, "", None))
      actor ! ExtractorRunner.SetTask(task)
      watch(actor)
      //expectMsg("Done!")
      //expectTerminated(actor)
      expectMsgAnyClassOf(classOf[String], classOf[Terminated])
      expectMsgAnyClassOf(classOf[String], classOf[Terminated])

      dataStore.ds.DB.readOnly { implicit session =>
        val numRecords = SQL(s"""SELECT COUNT(*) AS "count" FROM testtable;""")
          .map(rs => rs.long("count")).single.apply().get
        assert(numRecords >= 2)
      }

      execute(dataStore.ds, "DELETE FROM testtable WHERE a='I should be in the table';");
      execute(dataStore.ds, "DELETE FROM testtable WHERE a='I should also be in the table';");

      dataStore.ds.DB.readOnly { implicit session =>
        val numRecords = SQL(s"""SELECT COUNT(*) AS "count" FROM testtable;""")
          .map(rs => rs.long("count")).single.apply().get
        assert(numRecords === 0)
      }
    }
    
    it("should work for TSV extractor when output and input are from the same table"){

      execute(dataStore.ds, "drop table if exists testtable ;")
      execute(dataStore.ds, "create table testtable ( a text );") 
      val t = java.io.File.createTempFile("test", ".sh")
      val t2 = java.io.File.createTempFile("test", ".tsv")
      val t3 = java.io.File.createTempFile("test", ".py")
      t3.setExecutable(true, false)
      t2.setExecutable(true, false)

      log.info(t.getAbsolutePath)

      // Build the before script
      writeToFile(t, s"""
      | ${sqlScriptPrefix} "INSERT INTO testtable VALUES ('Mesasge_1'), ('Mesasge_2'), ('Mesasge_3'), ('Mesasge_4'), ('Mesasge_5'), ('Mesasge_6'), ('Mesasge_7'), ('Mesasge_8'), ('Mesasge_9'), ('Mesasge_10');"
      """.stripMargin)

      writeToFile(t3, 
     s"""|#! /usr/bin/python
         |import json, sys
         |for l in sys.stdin:
         |  print l.strip()
         |
      |""".stripMargin)

      val actor = system.actorOf(ExtractorRunner.props(dataStore, dbSettings))

      val task = new ExtractionTask(Extractor("testExtractor", "tsv_extractor", "testtable", 
        "SELECT * FROM testtable", t3.getAbsolutePath, 1, 1000, 1000, Nil.toSet, Some(t.getAbsolutePath), None, "", None))
      actor ! ExtractorRunner.SetTask(task)
      watch(actor)
      //expectMsg("Done!")
      //expectTerminated(actor)
      expectMsgAnyClassOf(classOf[String], classOf[Terminated])
      expectMsgAnyClassOf(classOf[String], classOf[Terminated])

      dataStore.ds.DB.readOnly { implicit session =>
        val numRecords = SQL(s"""SELECT COUNT(*) AS "count" FROM testtable;""")
          .map(rs => rs.long("count")).single.apply().get
        assert(numRecords === 20)
      }

      execute(dataStore.ds, "DELETE FROM testtable WHERE a='Mesasge_1';");
      execute(dataStore.ds, "DELETE FROM testtable WHERE a='Mesasge_2';");
      execute(dataStore.ds, "DELETE FROM testtable WHERE a='Mesasge_3';");
      execute(dataStore.ds, "DELETE FROM testtable WHERE a='Mesasge_4';");
      execute(dataStore.ds, "DELETE FROM testtable WHERE a='Mesasge_5';");
      execute(dataStore.ds, "DELETE FROM testtable WHERE a='Mesasge_6';");
      execute(dataStore.ds, "DELETE FROM testtable WHERE a='Mesasge_7';");
      execute(dataStore.ds, "DELETE FROM testtable WHERE a='Mesasge_8';");
      execute(dataStore.ds, "DELETE FROM testtable WHERE a='Mesasge_9';");
      execute(dataStore.ds, "DELETE FROM testtable WHERE a='Mesasge_10';");
     
      dataStore.ds.DB.readOnly { implicit session =>
        val numRecords = SQL(s"""SELECT COUNT(*) AS "count" FROM testtable;""")
          .map(rs => rs.long("count")).single.apply().get
        assert(numRecords === 0)
      }
    }

    it("should work for NULL escaping")(pending)

    it("should work for TSV extractor when input query contains multiple columns"){

      execute(dataStore.ds, "drop table if exists testtable ;")
      execute(dataStore.ds, "create table testtable ( a text, b text );") 
      val t = java.io.File.createTempFile("test", ".sh")
      val t2 = java.io.File.createTempFile("test", ".tsv")
      val t3 = java.io.File.createTempFile("test", ".py")
      t3.setExecutable(true, false)
      t2.setExecutable(true, false)

      log.info(t.getAbsolutePath)

      // Build the before script
      writeToFile(t, s"""
      | ${sqlScriptPrefix} "INSERT INTO testtable VALUES ('Mesasge_1', '1'), ('Mesasge_2', '1'), ('Mesasge_3', '1'), ('Mesasge_4', '1'), ('Mesasge_5', '1'), ('Mesasge_6', '1'), ('Mesasge_7', '1'), ('Mesasge_8', '1'), ('Mesasge_9', '1'), ('Mesasge_10', '1');"
      """.stripMargin)

      writeToFile(t3, 
     s"""|#! /usr/bin/python
         |import json, sys
         |for l in sys.stdin:
         |  print "\t".join(['abcdefg', "2"])
         |
      |""".stripMargin)


      val actor = system.actorOf(ExtractorRunner.props(dataStore, dbSettings))

      val task = new ExtractionTask(Extractor("testExtractor", "tsv_extractor", "testtable", 
        "SELECT * FROM testtable", t3.getAbsolutePath, 1, 1000, 1000, Nil.toSet, Some(t.getAbsolutePath), None, "", None))
      actor ! ExtractorRunner.SetTask(task)
      watch(actor)
      //expectMsg("Done!")
      //expectTerminated(actor)
      expectMsgAnyClassOf(classOf[String], classOf[Terminated])
      expectMsgAnyClassOf(classOf[String], classOf[Terminated])

      dataStore.ds.DB.readOnly { implicit session =>
        val numRecords = SQL(s"""SELECT COUNT(*) AS "count" FROM testtable;""")
          .map(rs => rs.long("count")).single.apply().get
        assert(numRecords === 20)
      }

      execute(dataStore.ds, "DELETE FROM testtable WHERE b='2';");
      dataStore.ds.DB.readOnly { implicit session =>
        val numRecords = SQL(s"""SELECT COUNT(*) AS "count" FROM testtable;""")
          .map(rs => rs.long("count")).single.apply().get
        assert(numRecords === 10)
      }
    }

    var exttype = "json_extractor"
    for(exttype <- List("json_extractor", "tsv_extractor")){
      it(s"should fail if extractor contain errors (${exttype})"){
        execute(dataStore.ds, "drop table if exists testtable ;")
        execute(dataStore.ds, "create table testtable ( a text, b text );") 
        val t = java.io.File.createTempFile("test", ".sh")
        val t2 = java.io.File.createTempFile("test", ".tsv")
        val t3 = java.io.File.createTempFile("test", ".py")
        t3.setExecutable(true, false)
        t2.setExecutable(true, false)

        log.info(t.getAbsolutePath)

      writeToFile(t, s"""
      | ${sqlScriptPrefix} "INSERT INTO testtable VALUES ('Mesasge_1', '1'), ('Mesasge_2', '1'), ('Mesasge_3', '1'), ('Mesasge_4', '1'), ('Mesasge_5', '1'), ('Mesasge_6', '1'), ('Mesasge_7', '1'), ('Mesasge_8', '1'), ('Mesasge_9', '1'), ('Mesasge_10', '1');"
      """.stripMargin)

        writeToFile(t3, 
       s"""|#! /usr/bin/python
           |import json, sys
           |for l in sys.stdin:
           |  print "\t".join(['abcdefg', "2"])
           |  lkdfjlkajflksajflkajflkjsaflkjalfjsaflksajfflkajflkasjflkajfl
           |
        |""".stripMargin)


        val actor = system.actorOf(ExtractorRunner.props(dataStore, dbSettings))

        val task = new ExtractionTask(Extractor("testExtractor", exttype, "testtable", 
          "SELECT * FROM testtable", t3.getAbsolutePath, 1, 1000, 1000, Nil.toSet, Some(t.getAbsolutePath), None, "", None))
        actor ! ExtractorRunner.SetTask(task)
        watch(actor)
        expectMsgAnyClassOf(classOf[Status.Failure], classOf[Terminated])
        expectMsgAnyClassOf(classOf[Status.Failure], classOf[Terminated])

      }

      it(s"should fail if SQL queries contain errors (${exttype})"){
        execute(dataStore.ds, "drop table if exists testtable ;")
        execute(dataStore.ds, "create table testtable ( a text, b text );") 
        val t = java.io.File.createTempFile("test", ".sh")
        val t2 = java.io.File.createTempFile("test", ".tsv")
        val t3 = java.io.File.createTempFile("test", ".py")
        t3.setExecutable(true, false)
        t2.setExecutable(true, false)

        log.info(t.getAbsolutePath)

        // Build the before script
        writeToFile(t, s"""
        | ${sqlScriptPrefix} "INSERT INTO testtable VALUES ('Mesasge_1', '1'), ('Mesasge_2', '1'), ('Mesasge_3', '1'), ('Mesasge_4', '1'), ('Mesasge_5', '1'), ('Mesasge_6', '1'), ('Mesasge_7', '1'), ('Mesasge_8', '1'), ('Mesasge_9', '1'), ('Mesasge_10', '1');"
        """.stripMargin)

        writeToFile(t3, 
            s"""|#! /usr/bin/python
                |import json, sys
                |for l in sys.stdin:
                |  print "\t".join(['abcdefg', "2"])
                |
                |""".stripMargin)


        val actor = system.actorOf(ExtractorRunner.props(dataStore, dbSettings))

        val task = new ExtractionTask(Extractor("testExtractor", exttype, "testtable", 
          "AAAAAAAAAAAAAAAAAAAAAAAAA * FROM testtable", t3.getAbsolutePath, 1, 1000, 1000, Nil.toSet, Some(t.getAbsolutePath), None, "", None))
        actor ! ExtractorRunner.SetTask(task)
        watch(actor)
        expectMsgAnyClassOf(classOf[Status.Failure], classOf[Terminated])
        expectMsgAnyClassOf(classOf[Status.Failure], classOf[Terminated])

      }
    }
  }


  describe("Running an SQL extractor"){

    it("should work for SQL extractor"){

      execute(dataStore.ds, "drop table if exists testtable ;")
      execute(dataStore.ds, "create table testtable ( a text );")
      val t = java.io.File.createTempFile("test", ".sh")
      val t2 = java.io.File.createTempFile("test", ".tsv")
      val t3 = java.io.File.createTempFile("test", ".py")
      t.setExecutable(true, false)
      t3.setExecutable(true, false)
      t2.setExecutable(true, false)

      log.info(t.getAbsolutePath)

      writeToFile(t, s"""
        | ${sqlScriptPrefix} "INSERT INTO testtable VALUES ('I should be in the table');"
        | ${sqlScriptPrefix} "INSERT INTO testtable VALUES ('I should also be in the table');"
      """.stripMargin)

      val actor = system.actorOf(ExtractorRunner.props(dataStore, dbSettings))

      val task = new ExtractionTask(Extractor("testExtractor", "sql_extractor", "testtable", 
        "DELETE FROM testtable WHERE a='I should be in the table';", "", 1, 1000, 1000, Nil.toSet, Some(t.getAbsolutePath), None, "DELETE FROM testtable WHERE a='I should be in the table';", None))
      actor ! ExtractorRunner.SetTask(task)
      watch(actor)
      //expectMsg("Done!")
      //expectTerminated(actor)
      expectMsgAnyClassOf(classOf[String], classOf[Terminated])
      expectMsgAnyClassOf(classOf[String], classOf[Terminated])

      dataStore.ds.DB.readOnly { implicit session =>
        val numRecords = SQL(s"""SELECT COUNT(*) AS "count" FROM testtable;""")
          .map(rs => rs.long("count")).single.apply().get
        assert(numRecords === 1)
      }

    }

    it("should fail for SQL extractor if SQL query is wrong"){

      execute(dataStore.ds, "drop table if exists testtable ;")
      execute(dataStore.ds, "create table testtable ( a text );")
      val t = java.io.File.createTempFile("test", ".sh")
      val t2 = java.io.File.createTempFile("test", ".tsv")
      val t3 = java.io.File.createTempFile("test", ".py")
      t.setExecutable(true, false)
      t3.setExecutable(true, false)
      t2.setExecutable(true, false)

      log.info(t.getAbsolutePath)

      writeToFile(t, s"""
        |${sqlScriptPrefix} "INSERT INTO testtable VALUES ('I should be in the table');"
        |${sqlScriptPrefix} "INSERT INTO testtable VALUES ('I should also be in the table');"
      """.stripMargin)

      val actor = system.actorOf(ExtractorRunner.props(dataStore, dbSettings))

      val task = new ExtractionTask(Extractor("testExtractor", "sql_extractor", "testtable", 
        "DELETEAAAAAA FROM testtable WHERE a='I should be in the table';", "", 1, 1000, 1000, Nil.toSet, Some(t.getAbsolutePath), None, "DELETEAAAAAA FROM testtable WHERE a='I should be in the table';", None))
      actor ! ExtractorRunner.SetTask(task)
      watch(actor)
      expectMsgAnyClassOf(classOf[Status.Failure], classOf[Terminated])
      expectMsgAnyClassOf(classOf[Status.Failure], classOf[Terminated])

    }
  
    
  }

  describe("Running an CMD extractor"){

    it("should work for CMD extractor"){

      execute(dataStore.ds, "drop table if exists testtable ;")
      execute(dataStore.ds, "create table testtable ( a text );")
      val t = java.io.File.createTempFile("test", ".sh")
      val t2 = java.io.File.createTempFile("test", ".tsv")
      val t3 = java.io.File.createTempFile("test", ".py")
      val t4 = java.io.File.createTempFile("test", ".sh")
      t.setExecutable(true, false)
      t3.setExecutable(true, false)
      t2.setExecutable(true, false)
      t4.setExecutable(true, false)

      log.info(t.getAbsolutePath)

      writeToFile(t, s"""
        |${sqlScriptPrefix} "INSERT INTO testtable VALUES ('I should be in the table');"
        |${sqlScriptPrefix} "INSERT INTO testtable VALUES ('I should also be in the table');"
      """.stripMargin)

      writeToFile(t4, s"""
        |${sqlScriptPrefix} "DELETE FROM testtable WHERE a='I should be in the table';"
      """.stripMargin)

      val actor = system.actorOf(ExtractorRunner.props(dataStore, dbSettings))

      val task = new ExtractionTask(Extractor("testExtractor", "cmd_extractor", "testtable", 
        t4.getAbsolutePath, "", 1, 1000, 1000, Nil.toSet, Some(t.getAbsolutePath), None, "", 
        Some(t4.getAbsolutePath)))
      actor ! ExtractorRunner.SetTask(task)
      watch(actor)
      //expectMsg("Done!")
      //expectTerminated(actor)
      expectMsgAnyClassOf(classOf[String], classOf[Terminated])
      expectMsgAnyClassOf(classOf[String], classOf[Terminated])

      dataStore.ds.DB.readOnly { implicit session =>
        val numRecords = SQL(s"""SELECT COUNT(*) AS "count" FROM testtable;""")
          .map(rs => rs.long("count")).single.apply().get
        assert(numRecords === 1)
      }

    }


    it("should fail for CMD extractor when the script contain errors"){

      execute(dataStore.ds, "drop table if exists testtable ;")
      execute(dataStore.ds, "create table testtable ( a text );")
      val t = java.io.File.createTempFile("test", ".sh")
      val t2 = java.io.File.createTempFile("test", ".tsv")
      val t3 = java.io.File.createTempFile("test", ".py")
      val t4 = java.io.File.createTempFile("test", ".sh")
      t.setExecutable(true, false)
      t3.setExecutable(true, false)
      t2.setExecutable(true, false)
      t4.setExecutable(true, false)

      log.info(t.getAbsolutePath)

      writeToFile(t, s"""
        |${sqlScriptPrefix} "INSERT INTO testtable VALUES ('I should be in the table');"
        |${sqlScriptPrefix} "INSERT INTO testtable VALUES ('I should also be in the table');"
      """.stripMargin)

      writeToFile(t4, s"""
        |${sqlScriptPrefix} "DELETEAAAAAAA FROM testtable WHERE a='I should be in the table';"
      """.stripMargin)

      val actor = system.actorOf(ExtractorRunner.props(dataStore, dbSettings))

      val task = new ExtractionTask(Extractor("testExtractor", "cmd_extractor", "testtable", 
        t4.getAbsolutePath, "", 1, 1000, 1000, Nil.toSet, Some(t.getAbsolutePath), None, "", 
        Some(t4.getAbsolutePath)))
      actor ! ExtractorRunner.SetTask(task)
      watch(actor)
      expectMsgAnyClassOf(classOf[Status.Failure], classOf[Terminated])
      expectMsgAnyClassOf(classOf[Status.Failure], classOf[Terminated])

    }

  }

}
