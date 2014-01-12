package org.deepdive.test.unit

import akka.actor._
import akka.testkit._
import org.deepdive.extraction._
import org.deepdive.extraction.ProcessExecutor._
import org.scalatest._
import scala.concurrent.duration._

class ProcessExecutorSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with FunSpecLike with BeforeAndAfter {

  def this() = this(ActorSystem("ProcessExecutorSpec"))

  describe("Running a process") {

    it("should work with a small batch size") {
      val actor = system.actorOf(ProcessExecutor.props)
      val probe = TestProbe()
      probe.watch(actor)
      actor.tell((Start("/bin/cat", 1)), probe.ref)
      actor.tell(Write("hello world"), probe.ref)
      actor.tell(Write("!!"), probe.ref)
      actor.tell(CloseInputStream, probe.ref)
      probe.expectMsg(OutputData(List("hello world")))
      probe.reply("Ok")
      probe.expectMsg(OutputData(List("!!")))
      probe.reply("Ok")
      probe.expectMsg(ProcessExited(0))
      probe. expectTerminated(actor)
    }

    it("should work with a larger batch size") {
      val actor = system.actorOf(ProcessExecutor.props)
      val probe = TestProbe()
      probe.watch(actor)
      actor.tell((Start("/bin/cat", 100)), probe.ref)
      actor.tell(Write("hello world"), probe.ref)
      actor.tell(Write("1"), probe.ref)
      actor.tell(Write("2"), probe.ref)
      actor.tell(CloseInputStream, probe.ref)
      probe.expectMsg(OutputData(List("hello world", "1", "2")))
      probe.reply("Ok")
      probe.expectMsg(ProcessExited(0))
      probe. expectTerminated(actor)
    }

    it("should work when the process crashes") {
      val failingExtractorFile = getClass.getResource("/failing_extractor.py").getFile
      val actor = system.actorOf(ProcessExecutor.props)
      watch(actor)
      actor ! (Start(failingExtractorFile, 100))
      actor ! Write("hello world")
      actor ! Write("1")
      actor ! Write("2")
      actor ! CloseInputStream
      expectMsg(ProcessExited(1))
      expectTerminated(actor)
    }

  }

}
