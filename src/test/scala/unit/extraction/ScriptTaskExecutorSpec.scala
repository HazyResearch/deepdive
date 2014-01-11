// package org.deepdive.test.unit

// import org.deepdive.extraction._
// import org.deepdive.settings.Extractor
// import org.scalatest._
// import play.api.libs.json._
// import play.api.libs.json._
// import rx.lang.scala.subjects._

// class ScriptTaskExecutorSpec extends FunSpec {

//   describe("Script Task Executor") {

//     it("should work with one batch") {
//       val task = new ExtractionTask(Extractor("testExtractor", "relation1", 
//         "relation1", "/bin/cat", 4, 100, 10000, Nil.toSet))
//       val data = (1 to 1000).toList.map(i => s"""{"id":$i}""".asJson.asJsObject).iterator
//       val executor = new ScriptTaskExecutor(task, data)
//       val outputSubject = ReplaySubject[Seq[JsObject]]()
//       executor.run(outputSubject)
//       assert(outputSubject.toBlockingObservable.toList.size == 1)
//       assert(outputSubject.toBlockingObservable.toList.flatten.size == 1000)
//     }

//     it("should work with multiple batches") {
//       val task = new ExtractionTask(Extractor("testExtractor", "relation1", 
//         "relation1", "/bin/cat", 4, 100, 100, Nil.toSet))
//       val data = (1 to 1000).toList.map(i => s"""{"id":$i}""".asJson.asJsObject).iterator
//             val executor = new ScriptTaskExecutor(task, data)
//       val outputSubject = ReplaySubject[Seq[JsObject]]()
//       executor.run(outputSubject)
//       assert(outputSubject.toBlockingObservable.toList.size == 10)
//       assert(outputSubject.toBlockingObservable.toList.flatten.size == 1000)
//     }

//     it("should work when the extractor fails") {
//       val failingExtractorFile = getClass.getResource("/failing_extractor.py").getFile
//       val task = new ExtractionTask(Extractor("testExtractor", "relation1", 
//         "relation1", failingExtractorFile, 4, 100, 100, Nil.toSet))
//       val data = (1 to 1000).toList.map(i => s"""{"id":$i}""".asJson.asJsObject).iterator
//       val executor = new ScriptTaskExecutor(task, data)
//       val outputSubject = ReplaySubject[Seq[JsObject]]()
//       executor.run(outputSubject)
//       intercept[RuntimeException] {
//         assert(outputSubject.toBlockingObservable.toList.size == 10)
//       }
//     }


//   }

// }