package org.deepdive.test.unit

import akka.actor._
import akka.testkit._
import org.deepdive.calibration._
import org.deepdive.inference._
import org.scalatest._
import scala.util.Success

class Forwarder(target: ActorRef) extends Actor {
  def receive = { case x => target.forward(x) }
}

class TestInferenceManager(
  val taskManager: ActorRef, 
  val samplerProbe: ActorRef,
  val factorGraphBuilderProbe: ActorRef,
  val cdwProbe: ActorRef,
  val variableSchema: Map[String, String]) 
  extends InferenceManager with MemoryInferenceDataStoreComponent {
    def factorGraphBuilderProps = Props(classOf[Forwarder], factorGraphBuilderProbe)
    override def samplerProps  = Props(classOf[Forwarder], samplerProbe)
    override def calibrationDataWriterProps = Props(classOf[Forwarder], cdwProbe)
  }

class InferenceManagerSpec(_system: ActorSystem) extends TestKit(_system) with FunSpecLike with ImplicitSender {

  def this() = this(ActorSystem("InferenceManagerSpec"))

  val taskManager = TestProbe()
  val sampler = TestProbe()
  val factorGraphBuilder = TestProbe()
  val cdw = TestProbe()
  val schema = Map("r1.c1" -> "Boolean", "r2.c1" -> "Boolean", "r2.c2" -> "Boolean")
  def actorProps = Props(classOf[TestInferenceManager], taskManager.ref, sampler.ref, 
    factorGraphBuilder.ref, cdw.ref, schema)

  describe("Executing a factor task") {
    it("should work") {
      val actor = TestActorRef(actorProps)
      actor ! InferenceManager.FactorTask(null, 0.0)
      factorGraphBuilder.expectMsgClass(classOf[FactorGraphBuilder.AddFactorsAndVariables])
      factorGraphBuilder.reply("Done")
      expectMsg("Done")
    }
  }

  describe("Running inference") {
    it("should work") {
      val actor = TestActorRef(actorProps)
      actor ! InferenceManager.RunInference("javaArgs", "samplerOptions")
      sampler.expectMsgClass(classOf[Sampler.Run])
      sampler.reply("Done")
      expectMsg(Success())
    }
  }

  describe("Writing calibration data") {
    it("should work") {
      val actor = TestActorRef(actorProps)
      actor ! InferenceManager.WriteCalibrationData
      cdw.expectMsgClass(classOf[CalibrationDataWriter.WriteCalibrationData])
      cdw.reply("Done")
      cdw.expectMsgClass(classOf[CalibrationDataWriter.WriteCalibrationData])
      cdw.reply("Done")
      cdw.expectMsgClass(classOf[CalibrationDataWriter.WriteCalibrationData])
      cdw.reply("Done")
      expectMsg(Set("Done"))
    }
  }

}