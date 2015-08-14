package org.deepdive.test.unit

import akka.actor._
import akka.testkit._
import org.deepdive.settings.{Extractor, DbSettings}
import org.deepdive.extraction._
import org.deepdive.datastore._
import org.deepdive.helpers.Helpers
import org.deepdive.test.helpers._
import org.deepdive.Logging
import org.scalatest._
import scala.util.{Try, Success, Failure}

/* Stores Extraction Results */
trait MemoryDataStoreComponent extends JdbcDataStoreComponent{
  val dataStore = new MemoryDataStore
}

class MemoryDataStore extends JdbcDataStore {
}

object ExtractionManagerSpec {
  class MemoryExtractionManager(val parallelism: Int, val dbSettings: DbSettings) extends ExtractionManager with
    MemoryDataStoreComponent {

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
      val someExtractor = Extractor(
        name = "e1",
        style = "json_extractor",
        outputRelation = "r1",
        inputQuery = "query",
        udfDir = null,
        udf = "udf",
        parallelism = 3,
        inputBatchSize = 1000,
        outputBatchSize = 1000,
        dependencies = Set(),
        beforeScript = None,
        afterScript = None,
        sqlQuery = "query",
        cmd = None
      )
      manager ! ExtractionTask(someExtractor)
      expectMsg("Done!")
    }

    it("should execute tasks when parallelism=1") {
      val manager = TestActorRef[MemoryExtractionManager](Props(classOf[MemoryExtractionManager], 1, dbSettings))
      val someExtractor = Extractor(
        name = "e1",
        style = "json_extractor",
        outputRelation = "r1",
        inputQuery = "query",
        udfDir = null,
        udf = "udf",
        parallelism = 3,
        inputBatchSize = 1000,
        outputBatchSize = 1000,
        dependencies = Set(),
        beforeScript = None,
        afterScript = None,
        sqlQuery = "query",
        cmd = None
      )
      manager ! ExtractionTask(someExtractor)
      manager ! ExtractionTask(someExtractor.copy(name="e2"))
      manager ! ExtractionTask(someExtractor.copy(name="e3"))
      expectMsg("Done!")
      expectMsg("Done!")
      expectMsg("Done!")
    }

    it("should execute tasks when paralleism > 1") {
      val manager = TestActorRef[MemoryExtractionManager](Props(classOf[MemoryExtractionManager], 4, dbSettings))
      val someExtractor = Extractor(
        name = "e1",
        style = "json_extractor",
        outputRelation = "r1",
        inputQuery = "query",
        udfDir = null,
        udf = "udf",
        parallelism = 3,
        inputBatchSize = 1000,
        outputBatchSize = 1000,
        dependencies = Set(),
        beforeScript = None,
        afterScript = None,
        sqlQuery = "query",
        cmd = None
      )
      manager ! ExtractionTask(someExtractor)
      manager ! ExtractionTask(someExtractor.copy(name="e2"))
      manager ! ExtractionTask(someExtractor.copy(name="e3"))
      expectMsg("Done!")
      expectMsg("Done!")
      expectMsg("Done!")
    }

  }

}
