package org.deepdive.test.unit

import akka.actor._
import akka.testkit._
import org.deepdive.settings.Extractor
import org.deepdive.extraction._
import org.deepdive.extraction.datastore._
import org.scalatest._
import scala.util.{Try, Success, Failure}

object ExtractionManagerSpec {
  class MemoryExtractionManager(val parallelism: Int) extends ExtractionManager with
    MemoryExtractionDataStoreComponent {

    override def extractorExecutorProps = Props(new Actor {
      def receive = {
        case ExtractorExecutor.ExecuteTask(task) =>
          Thread.sleep(100) 
          sender ! ExtractionTaskResult(task, Success())
          context.stop(self)
      }
    })

  }
}

class ExtractionManagerSpec(_system: ActorSystem) extends TestKit(_system) 
  with FunSpecLike with BeforeAndAfter with ImplicitSender {
  import ExtractionManagerSpec._

  def this() = this(ActorSystem("ExtractionManagerSpec"))

  describe("Extraction Manager") {
    
    it("should execute one task") {
      val manager = TestActorRef[MemoryExtractionManager](Props(classOf[MemoryExtractionManager], 1))
      val someExtractor = Extractor("e1", "r1", "query", "udf", 3, 1000, Set())
      manager ! ExtractionTask(someExtractor)
      expectMsg(Success())
    }

    it("should execute tasks when parallelism=1") {
      val manager = TestActorRef[MemoryExtractionManager](Props(classOf[MemoryExtractionManager], 1))
      val someExtractor = Extractor("e1", "r1", "query", "udf", 3, 1000, Set())
      manager ! ExtractionTask(someExtractor)
      manager ! ExtractionTask(someExtractor.copy(name="e2"))
      manager ! ExtractionTask(someExtractor.copy(name="e3"))
      expectMsg(Success())
      expectMsg(Success())
      expectMsg(Success())
    }

    it("should execute tasks when paralleism > 1") {
      val manager = TestActorRef[MemoryExtractionManager](Props(classOf[MemoryExtractionManager], 4))
      val someExtractor = Extractor("e1", "r1", "query", "udf", 3, 1000, Set())
      manager ! ExtractionTask(someExtractor)
      manager ! ExtractionTask(someExtractor.copy(name="e2"))
      manager ! ExtractionTask(someExtractor.copy(name="e3"))
      expectMsg(Success())
      expectMsg(Success())
      expectMsg(Success())
    }

  }

}