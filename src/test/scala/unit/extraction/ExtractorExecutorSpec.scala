// package org.deepdive.test.unit

// // import akka.actor._
// import akka.actor._
// import akka.testkit.{TestActorRef, TestKit}
// import org.scalatest._
// import org.deepdive.extraction._
// import org.deepdive.extraction.datastore._
// import org.deepdive.settings.Extractor
// import play.api.libs.json._
// import play.api.libs.json._

// class ExtractorExecutorSpec extends FunSpec with BeforeAndAfter 
//   with MemoryExtractionDataStoreComponent  {

//   implicit val system = ActorSystem("test")

//   describe("ExtractorExecutor") {

//     it("should be able to execute an extraction task") {
//       val testActor = TestActorRef[ExtractorExecutor](Props(classOf[ExtractorExecutor], dataStore)).underlyingActor
//       // Add test record to the data store
//       dataStore.addBatch(List("""{"id": 5}""".asJson.asJsObject), "relation1")
//       val task = new ExtractionTask(Extractor("testExtractor", "relation1", 
//         "relation1", "/bin/cat", 1, 1000, 1000, Nil.toSet))
//       val result = testActor.doExecute(task)
//       assert(result.isSuccess)
//       assert(dataStore.data("relation1").size == 2)
//     }

//     it("should return failure when the task fails") {
//       val failingExtractorFile = getClass.getResource("/failing_extractor.py").getFile
//       val testActor = TestActorRef[ExtractorExecutor](Props(classOf[ExtractorExecutor], dataStore)).underlyingActor
//       // Add test record to the data store
//       dataStore.addBatch(List("""{"id": 5}""".asJson.asJsObject), "relation1")
//       val task = new ExtractionTask(Extractor("testExtractor", "relation1", 
//         "relation1", failingExtractorFile, 1, 1000, 1000, Nil.toSet))
//       val result = testActor.doExecute(task)
//       assert(result.isFailure)
//     }

//     it("should successfully execute before and after scripts") {
//       val testActor = TestActorRef[ExtractorExecutor](
//         Props(classOf[ExtractorExecutor], dataStore)).underlyingActor
//       val task = new ExtractionTask(Extractor("testExtractor", "relation1", 
//         "relation1", "/bin/cat", 1, 1000, 1000, Nil.toSet, Option("echo Hello"), Option("echo World")))
//       val result = testActor.doExecute(task)
//       assert(result.isSuccess)
//     }

//     it("should return a failure when the before or afer script doesn't exit with value=0") {
//       val testActor = TestActorRef[ExtractorExecutor](
//         Props(classOf[ExtractorExecutor], dataStore)).underlyingActor
//       val task = new ExtractionTask(Extractor("testExtractor", "relation1", 
//         "relation1", "/bin/cat", 1, 1000, 1000, Nil.toSet, Option("ls NO!"), Option("echo World")))
//       val result = testActor.doExecute(task)
//       assert(result.isFailure)
//     }

//     it("should return a failure when the before or after script crashes") {
//       val testActor = TestActorRef[ExtractorExecutor](
//         Props(classOf[ExtractorExecutor], dataStore)).underlyingActor
//       val task = new ExtractionTask(Extractor("testExtractor", "relation1", 
//         "relation1", "/bin/cat", 1, 1000, 1000, Nil.toSet, Option("/bin/OHNO!"), Option("echo World")))
//       val result = testActor.doExecute(task)
//       assert(result.isFailure)
//     }

//   }

// }