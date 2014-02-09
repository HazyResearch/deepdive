package org.deepdive.udf.nlp.test

import java.io._
import org.deepdive.udf.nlp._
import org.scalatest._
import play.api.libs.json._
import scala.io.Source
import java.util.Properties

class DocumentParserSpec extends FunSpec {

  describe("Parsing documents") {

    it("should work with plain text") {
      val inputFile = getClass.getResource("/testdoc.txt").getFile
      val documentStr = Source.fromFile(inputFile).mkString
      
      val props = new Properties()
      props.put("annotators", "tokenize, cleanxml, ssplit, pos, lemma, ner, parse, dcoref")
      val dp = new DocumentParser(props)

      val result = dp.parseDocumentString(documentStr)
      assert(result.sentences.size == 3)
    }

    it("should work with HTML documents") {
      val inputFile = getClass.getResource("/testdoc.html").getFile
      val documentStr = Source.fromFile(inputFile).mkString

      val props = new Properties()
      props.put("annotators", "tokenize, cleanxml, ssplit, pos, lemma, ner, parse, dcoref")
      val dp = new DocumentParser(props)

      val result = dp.parseDocumentString(documentStr)
      assert(result.sentences.size == 23)
    }

  }

  describe("Running the main method from the command line") {

    it("should work with valid JSON") {
      // Read stdin from file
      val inputFile = getClass.getResource("/input.json.txt").getFile
      val is = new FileInputStream(inputFile)
      System.setIn(is)
      
      // Execute the main method
      Main.main(Array("--valueKey", "documents.text", "--idKey", "documents.id"))
    } 
  }

}