package org.deepdive.test.unit

// import akka.actor._
import akka.actor._
import akka.testkit.{TestActorRef, TestKit}
import org.scalatest._
import org.deepdive.extraction._
import org.deepdive.extraction.datastore._
import org.deepdive.settings.Extractor
import spray.json._
import spray.json.DefaultJsonProtocol._

class ExtractorExecutorSpec extends FunSpec with BeforeAndAfter 
  with MemoryExtractionDataStoreComponent  {

  implicit val system = ActorSystem("test")

  describe("ExtractorExecutor") {

    it("should be able to execute an extraction task") {
      val testActor = TestActorRef[ExtractorExecutor](Props(classOf[ExtractorExecutor], dataStore)).underlyingActor
      // Add test record to the data store
      dataStore.write(List("""{"id": 5}""".asJson.asJsObject), "relation1")
      val task = new ExtractionTask(Extractor("testExtractor", "relation1", 
        "relation1", "/bin/cat", 1, 1000, Nil.toSet))
      val result = testActor.doExecute(task)
      assert(result.result.isSuccess)
      assert(dataStore.data("relation1").size == 2)
    }

  }

}