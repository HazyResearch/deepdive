package org.deepdive.test.unit

import akka.actor._
import akka.testkit._
import org.deepdive.profiling._
import org.scalatest._

class ProfilerSpec(_system: ActorSystem) extends TestKit(_system) with FunSpecLike with ImplicitSender {

  def this() = this(ActorSystem("ProfilerSpec"))

  describe("Assembling reports") {

    it("should work") {
      val actor = TestActorRef[Profiler]
      actor.receive(StartReport("1", "Report 1"))
      actor.receive(EndReport("1", Option("Done!")))
      assert(actor.underlyingActor.reports.size == 1)
    }

  }

} 