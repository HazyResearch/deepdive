package org.deepdive.test.unit

import akka.actor._
import akka.testkit._
import org.deepdive.extraction._
import org.deepdive.extraction.datastore._
import org.deepdive.extraction.ProcessExecutor._
import org.deepdive.settings.Extractor
import org.scalatest._
import scala.concurrent.duration._
import play.api.libs.json._

class ExtractorRunnerSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with FunSpecLike with BeforeAndAfter with MemoryExtractionDataStoreComponent {

  before {
    dataStore.init()
  }  

  def this() = this(ActorSystem("ExtractorRunnerSpec"))

  describe("Running an extraction task with not parallelism") {

    it("should work without parallelism") {
      val actor = system.actorOf(ExtractorRunner.props(dataStore))
      dataStore.addBatch(List(Json.parse("""{"id": 5}""").asInstanceOf[JsObject]).iterator, "relation1")
      val task = new ExtractionTask(Extractor("testExtractor", "relation1", 
        "relation1", "/bin/cat", 1, 1000, 1000, Nil.toSet))
      actor ! ExtractorRunner.SetTask(task)
      watch(actor)
      expectMsg("Done!")
      expectTerminated(actor)
      assert(dataStore.data("relation1").size == 2)
    }

    it("should work when the input is empty") {
      val actor = system.actorOf(ExtractorRunner.props(dataStore))
      val task = new ExtractionTask(Extractor("testExtractor", "relation1", 
        "relation1", "/bin/cat", 1, 1000, 1000, Nil.toSet))
      actor ! ExtractorRunner.SetTask(task)
      watch(actor)
      expectMsg("Done!")
      expectTerminated(actor)
      assert(dataStore.data.get("relation1") === None)
    }

    it("should work with parallelism") {
      val actor = system.actorOf(ExtractorRunner.props(dataStore))
      for (i <- (1 to 1000)) {
        dataStore.addBatch(List(Json.parse(s"""{"id": ${i}}""").asInstanceOf[JsObject]).iterator, "relation1")
      }
      val task = new ExtractionTask(Extractor("testExtractor", "relation1", 
        "relation1", "/bin/cat", 10, 10, 10, Nil.toSet))
      actor ! ExtractorRunner.SetTask(task)
      watch(actor)
      expectMsg("Done!")
      expectTerminated(actor)
      assert(dataStore.data("relation1").size == 2000)
    }

    it("should return failure when the task failes") {
      val actor = system.actorOf(ExtractorRunner.props(dataStore))
      val failingExtractorFile = getClass.getResource("/failing_extractor.py").getFile
      dataStore.addBatch(List(Json.parse("""{"id": 5}""").asInstanceOf[JsObject]).iterator, "relation1")
      val task = new ExtractionTask(Extractor("testExtractor", "relation1", 
        "relation1", failingExtractorFile, 1, 1000, 1000, Nil.toSet))
      actor ! ExtractorRunner.SetTask(task)
      watch(actor)
      expectMsgClass(classOf[Status.Failure])
      expectTerminated(actor)
    }

    it("should correctly execute the before and after scripts") {
      val actor = system.actorOf(ExtractorRunner.props(dataStore))
      val task = new ExtractionTask(Extractor("testExtractor", "relation1", 
        "relation1", "/bin/cat", 1, 1000, 1000, Nil.toSet, Option("echo Hello"), Option("echo World")))
      actor ! ExtractorRunner.SetTask(task)
      watch(actor)
      expectMsg("Done!")
      expectTerminated(actor)
    }

    it("should return a failure when the before script crashes") {
      val actor = system.actorOf(ExtractorRunner.props(dataStore))
      val task = new ExtractionTask(Extractor("testExtractor", "relation1", 
        "relation1", "/bin/cat", 1, 1000, 1000, Nil.toSet, Option("/bin/OHNO!"), Option("echo World")))
      actor ! ExtractorRunner.SetTask(task)
      watch(actor)
      expectMsgClass(classOf[Status.Failure])
      expectTerminated(actor)
    }

    it("should return a failure when the after script crashes") {
      val actor = system.actorOf(ExtractorRunner.props(dataStore))
      val task = new ExtractionTask(Extractor("testExtractor", "relation1", 
        "relation1", "/bin/cat", 1, 1000, 1000, Nil.toSet, Option("echo Hello"), Option("/bin/OHNO!")))
      actor ! ExtractorRunner.SetTask(task)
      watch(actor)
      expectMsgClass(classOf[Status.Failure])
      expectTerminated(actor)
    }

  }


}