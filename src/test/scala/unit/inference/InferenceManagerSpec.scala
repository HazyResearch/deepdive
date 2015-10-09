package org.deepdive.test.unit

import akka.actor._
import akka.testkit._
import org.deepdive.inference._
import org.deepdive.helpers.Helpers
import org.deepdive.test.helpers._
import org.deepdive.settings._
import org.scalatest._
import org.deepdive.settings.{BooleanType, VariableDataType, DbSettings}
import scala.util.Success

class Forwarder(target: ActorRef) extends Actor {
  def receive = { case x => target.forward(x) }
}

class TestInferenceManager(

  val taskManager: ActorRef,
  val samplerProbe: ActorRef,
  val factorGraphBuilderProbe: ActorRef,
  val cdwProbe: ActorRef,
  val variableSchema: Map[String, _ <: VariableDataType],
  val dbSettings: DbSettings
  )
  extends InferenceManager with MemoryInferenceRunnerComponent {
    override def samplerProps  = Props(classOf[Forwarder], samplerProbe)
  }

class InferenceManagerSpec(_system: ActorSystem) extends TestKit(_system) with FunSpecLike with ImplicitSender {

  def this() = this(ActorSystem("InferenceManagerSpec"))
  val dbSettings = TestHelper.getDbSettings

  val taskManager = TestProbe()
  val sampler = TestProbe()
  val factorGraphBuilder = TestProbe()
  val cdw = TestProbe()
  val schema = Map("r1.c1" -> BooleanType, "r2.c1" -> BooleanType, "r2.c2" -> BooleanType)
  def actorProps = Props(classOf[TestInferenceManager], taskManager.ref, sampler.ref,
    factorGraphBuilder.ref, cdw.ref, schema, dbSettings)

  describe("Running inference") {
    it("should work") {
      val actor = TestActorRef(actorProps)
      actor ! InferenceManager.RunInference(Seq(), 0.0, None, "javaArgs", "samplerOptions",
        PipelineSettings(
          activePipelineName = None,
          pipelines = Nil,
          relearnFrom = null,
          baseDir = None
        ), dbSettings)
      sampler.expectMsgClass(classOf[Sampler.Run])
      sampler.reply("Done")
      expectMsg(())
    }
  }

}
