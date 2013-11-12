import org.scalatest._
import scala.io.Source

import org.deepdive.extraction._
import spray.json._
import DefaultJsonProtocol._

class ScriptTaskExecutorSpec extends FunSpec {

  it("should work with a basic query") {
    val databaseUrl = "jdbc:postgresql://localhost/deepdive_paleo"
    val extractorFile = getClass.getResource("/simple_extractor.py")
    val task = ExtractionTask("test", "output", "SELECT id, docid FROM documents", extractorFile.getFile)
    val executor = new ScriptTaskExecutor(task, databaseUrl)
    val result = executor.run()
    assert(result.head.compactPrint == List("469","469").toJson.compactPrint)
  }

}