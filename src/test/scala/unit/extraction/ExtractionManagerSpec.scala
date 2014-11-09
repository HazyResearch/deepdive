package org.deepdive.test.unit

import akka.actor._
import akka.testkit._
import org.deepdive.settings.{Extractor, DbSettings}
import org.deepdive.extraction._
import org.deepdive.extraction.datastore._
import org.deepdive.helpers.Helpers
import org.deepdive.test.helpers._
import org.scalatest._
import scala.util.{Try, Success, Failure}

object ExtractionManagerSpec {
  class MemoryExtractionManager(val parallelism: Int, val dbSettings: DbSettings) extends ExtractionManager with
    MemoryExtractionDataStoreComponent {

    override def extractorRunnerProps = Props(new Actor {
      def receive = {
        case ExtractorRunner.SetTask(task) =>
          Thread.sleep(100) 
          sender ! "Done!"
          context.stop(self)
      }
    })

  }
}

class ExtractionManagerSpec(_system: ActorSystem) extends TestKit(_system) 
  with FunSpecLike with BeforeAndAfter with ImplicitSender {
  import ExtractionManagerSpec._

  def this() = this(ActorSystem("ExtractionManagerSpec"))
  val dbSettings = TestHelper.getDbSettings

  describe("Extraction Manager") {
    
    it("should execute one task") {
      val manager = TestActorRef[MemoryExtractionManager](Props(classOf[MemoryExtractionManager], 1, dbSettings))
      val someExtractor = Extractor("e1", "json_extractor", "r1", "query", "udf", 3, 1000, 1000, Set(),
        None, None, "query", None)
      manager ! ExtractionTask(someExtractor)
      expectMsg("Done!")
    }

    it("should execute tasks when parallelism=1") {
      val manager = TestActorRef[MemoryExtractionManager](Props(classOf[MemoryExtractionManager], 1, dbSettings))
      val someExtractor = Extractor("e1", "json_extractor", "r1", "query", "udf", 3, 1000, 1000, Set(),
         None, None, "query", None)
      manager ! ExtractionTask(someExtractor)
      manager ! ExtractionTask(someExtractor.copy(name="e2"))
      manager ! ExtractionTask(someExtractor.copy(name="e3"))
      expectMsg("Done!")
      expectMsg("Done!")
      expectMsg("Done!")
    }

    it("should execute tasks when paralleism > 1") {
      val manager = TestActorRef[MemoryExtractionManager](Props(classOf[MemoryExtractionManager], 4, dbSettings))
      val someExtractor = Extractor("e1", "json_extractor", "r1", "query", "udf", 3, 1000, 1000, Set(),
         None, None, "query", None)
      manager ! ExtractionTask(someExtractor)
      manager ! ExtractionTask(someExtractor.copy(name="e2"))
      manager ! ExtractionTask(someExtractor.copy(name="e3"))
      expectMsg("Done!")
      expectMsg("Done!")
      expectMsg("Done!")
    }

  }

}