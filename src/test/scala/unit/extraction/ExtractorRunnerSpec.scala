package org.deepdive.test.unit

import akka.actor._
import akka.testkit._
import org.deepdive.datastore._
import org.deepdive.extraction._
import org.deepdive.extraction.datastore._
import org.deepdive.extraction.ProcessExecutor._
import org.deepdive.settings._
import org.scalatest._
import scala.concurrent.duration._
import play.api.libs.json._
import scalikejdbc._

class ExtractorRunnerSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with FunSpecLike with BeforeAndAfter with PostgresExtractionDataStoreComponent {

  before {
    JdbcDataStore.init()
    dataStore.init()
    dataStore.ds.DB.autoCommit { implicit session =>
      SQL("drop schema if exists public cascade; create schema public;").execute.apply()
      SQL("""CREATE TABLE relation1(id bigint, key int);""").execute.apply()     
    }
  } 

  after {
    JdbcDataStore.close()
  }

  def this() = this(ActorSystem("ExtractorRunnerSpec"))

  val dbSettings = DbSettings(null, null, null, null, null, null, null, null, null, null)

  // lazy implicit val session = ds.DB.autoCommitSession()

  describe("Running an json_extractor extraction task") {

    it("should work without parallelism") {
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
      expectMsg("Done!")
      expectTerminated(actor)
      dataStore.ds.DB.readOnly { implicit session =>
        val numRecords = SQL(s"""SELECT COUNT(*) AS "count" FROM relation1""")
          .map(rs => rs.long("count")).single.apply().get
        assert(numRecords === 2)
      }
    }

    it("should work when the input is empty") {
      val actor = system.actorOf(ExtractorRunner.props(dataStore, dbSettings))
      val task = new ExtractionTask(Extractor("testExtractor", "json_extractor", "relation1", 
        "SELECT * FROM relation1", "/bin/cat", 1, 1000, 1000, Nil.toSet))
      actor ! ExtractorRunner.SetTask(task)
      watch(actor)
      expectMsg("Done!")
      expectTerminated(actor)
      dataStore.ds.DB.readOnly { implicit session =>
        val numRecords = SQL(s"""SELECT COUNT(*) AS "count" FROM relation1""")
          .map(rs => rs.long("count")).single.apply().get
        assert(numRecords === 0)
      }
    }

    // it("should work with parallelism") {
    //   val actor = system.actorOf(ExtractorRunner.props(dataStore, dbSettings))

    //   val batchData = (1 to 1000).map { i =>
    //     Json.parse(s"""{"key": ${i}}""").asInstanceOf[JsObject]
    //   }.toList
    //   dataStore.addBatch(batchData.iterator, "relation1")
    //   val task = new ExtractionTask(Extractor("testExtractor", "relation1", 
    //     "SELECT * FROM relation1", "/bin/cat", 4, 500, 200, Nil.toSet))
    //   actor ! ExtractorRunner.SetTask(task)
    //   watch(actor)
    //   expectMsg("Done!")
    //   expectTerminated(actor)
    //   dataStore.ds.DB.readOnly { implicit session =>
    //     val numRecords = SQL(s"""SELECT COUNT(*) AS "count" FROM relation1""")
    //       .map(rs => rs.long("count")).single.apply().get
    //     assert(numRecords === 2000)
    //   }
    // }

    // it("should return failure when the task failes") {
    //   val actor = system.actorOf(ExtractorRunner.props(dataStore, dbSettings))
    //   val failingExtractorFile = getClass.getResource("/failing_extractor.py").getFile
    //   dataStore.addBatch(List(Json.parse("""{"key": 5}""").asInstanceOf[JsObject]).iterator, "relation1")
    //   val task = new ExtractionTask(Extractor("testExtractor", "relation1", 
    //     "SELECT * FROM relation1", failingExtractorFile, 1, 1000, 1000, Nil.toSet))
    //   actor ! ExtractorRunner.SetTask(task)
    //   watch(actor)
    //   expectMsgClass(classOf[Status.Failure])
    //   expectTerminated(actor)
    // }

    // it("should correctly execute the before and after scripts") {
    //   val actor = system.actorOf(ExtractorRunner.props(dataStore, dbSettings))
    //   val task = new ExtractionTask(Extractor("testExtractor", "relation1", 
    //     "SELECT * FROM relation1", "/bin/cat", 1, 1000, 1000, Nil.toSet, Option("echo Hello"), Option("echo World")))
    //   actor ! ExtractorRunner.SetTask(task)
    //   watch(actor)
    //   expectMsg("Done!")
    //   expectTerminated(actor)
    // }

    // it("should return a failure when the query is invalid") {
    //   val actor = system.actorOf(ExtractorRunner.props(dataStore, dbSettings))
    //   val task = new ExtractionTask(Extractor("testExtractor", "relation5", 
    //     "relation1", "/bin/cat", 1, 1000, 1000, Nil.toSet))
    //   actor ! ExtractorRunner.SetTask(task)
    //   watch(actor)
    //   expectMsgClass(classOf[Status.Failure])
    //   expectTerminated(actor)
    // }

    // it("should return a failure when the before script crashes") {
    //   val actor = system.actorOf(ExtractorRunner.props(dataStore, dbSettings))
    //   val task = new ExtractionTask(Extractor("testExtractor", "relation1", 
    //     "SELECT * FROM relation1", "/bin/cat", 1, 1000, 1000, Nil.toSet, Option("/bin/OHNO!"), Option("echo World")))
    //   actor ! ExtractorRunner.SetTask(task)
    //   watch(actor)
    //   expectMsgClass(classOf[Status.Failure])
    //   expectTerminated(actor)
    // }

    // it("should return a failure when the after script crashes") {
    //   val actor = system.actorOf(ExtractorRunner.props(dataStore, dbSettings))
    //   val task = new ExtractionTask(Extractor("testExtractor", "relation1", 
    //     "SELECT * FROM relation1", "/bin/cat", 1, 1000, 1000, Nil.toSet, Option("echo Hello"), Option("/bin/OHNO!")))
    //   actor ! ExtractorRunner.SetTask(task)
    //   watch(actor)
    //   expectMsgClass(classOf[Status.Failure])
    //   expectTerminated(actor)
    // }

  }


}