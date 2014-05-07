package org.deepdive.test.unit

import akka.actor._
import akka.testkit._
import org.deepdive.profiling._
import org.deepdive.{Task, TaskManager}
import org.scalatest._
import scala.util.Success

class TaskManagerSpec(_system: ActorSystem) extends TestKit(_system) 
  with ImplicitSender with FunSpecLike {

  def this() = this(ActorSystem("TaskManagerSpec"))

  describe("Scheduling tasks") {

    it("should schedule tasks that have no dependencies") {
      val actor = TestActorRef[TaskManager].underlyingActor
      actor.taskQueue += Task("task1", List(), "someTask", TestProbe().ref)
      actor.scheduleTasks()
      assert(actor.taskQueue.size == 0)
      assert(actor.runningTasks.size == 1)
    }

    it("should not schedule tasks that don't have their depenencies satisfied") {
      val actor = TestActorRef[TaskManager].underlyingActor
      actor.taskQueue += Task("task1", List("task2"), "someTask", TestProbe().ref)
      actor.scheduleTasks()
      assert(actor.taskQueue.size == 1)
      assert(actor.runningTasks.size == 0)
    }

    it("should schedule tasks that have their depenencies satisfied") {
      val actor = TestActorRef[TaskManager].underlyingActor
      actor.completedTasks += TaskManager.Done(
        Task("task1", List(), "someTask", TestProbe().ref), Success())
      actor.taskQueue += Task("task2", List("task1"), "someTask", TestProbe().ref)
      actor.scheduleTasks()
      assert(actor.taskQueue.size == 0)
      assert(actor.runningTasks.size == 1)
    }

  }

  describe("Adding tasks") {
    it("should work") {
      val actor = TestActorRef[TaskManager]
      val worker = TestProbe()
      actor ! TaskManager.AddTask(Task("task1", Nil, "someTask", worker.ref))
      worker.expectMsg("someTask")
      assert(actor.underlyingActor.taskQueue.size == 0)
      assert(actor.underlyingActor.runningTasks.size == 1)
    }
  }

  describe("Completing Tasks") {
    it("should work for succeeded tasks") {
      _system.eventStream.subscribe(self, classOf[ReportingEvent])
      val actor = TestActorRef[TaskManager]
      val worker = TestProbe()
      actor ! TaskManager.AddTask(Task("task1", Nil, "someTask", worker.ref))
      worker.expectMsg("someTask")
      expectMsg(StartReport("task1", "task1"))
      worker.reply("Done!")
      expectMsg(EndReport("task1", Option("SUCCESS")))
    }

    it("should not work for failed tasks")(pending)
    it("should not work for actors that are being terminated")(pending)
    // SHOULD FAIL IF TASK FAILS
    // it("should work for failed tasks") {
    //   _system.eventStream.subscribe(self, classOf[ReportingEvent])
    //   val actor = TestActorRef[TaskManager]
    //   val worker = TestProbe()
    //   actor ! TaskManager.AddTask(Task("task1", Nil, "someTask", worker.ref))
    //   expectMsg(StartReport("task1", "task1"))
    //   worker.expectMsg("someTask")
    //   worker.reply(akka.actor.Status.Failure(new RuntimeException("!")))
    //   expectMsg(EndReport("task1", Option("FAILURE")))
    // }

    // SHOULD NOT WORK FOR ACTORS THAT ARE BEING TERMINATED
    // it("should work for actors that are being terminated") {
    //   _system.eventStream.subscribe(self, classOf[ReportingEvent])
    //   val actor = TestActorRef[TaskManager]
    //   val worker = TestProbe()
    //   actor ! TaskManager.AddTask(Task("task1", Nil, "someTask", worker.ref))
    //   expectMsg(StartReport("task1", "task1"))
    //   worker.expectMsg("someTask")
    //   worker.ref ! PoisonPill
    //   worker.reply(akka.actor.Status.Failure(new RuntimeException("!")))
    //   expectMsg(EndReport("task1", Option("FAILURE")))
    // }
  }

  describe("Subscribing to tasks") {
    it("it not being used")(pending)
  }

  describe("Unsubscribing from tasks") {
    it("it not being used")(pending)
  }



}