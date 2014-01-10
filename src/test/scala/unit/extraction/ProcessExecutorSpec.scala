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
      watch(actor)
      actor ! (Start("/bin/cat", 1))
      actor ! Write("hello world")
      actor ! Write("!!")
      actor ! CloseInputStream
      expectMsg(OutputData(List("hello world")))
      expectMsg(OutputData(List("!!")))
      expectMsg(ProcessExited(0))
      expectTerminated(actor)
    }

    it("should work with a larger batch size") {
      val actor = system.actorOf(ProcessExecutor.props)
      watch(actor)
      actor ! (Start("/bin/cat", 100))
      actor ! Write("hello world")
      actor ! Write("1")
      actor ! Write("2")
      actor ! CloseInputStream
      expectMsg(OutputData(List("hello world", "1", "2")))
      expectMsg(ProcessExited(0))
      expectTerminated(actor)
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
