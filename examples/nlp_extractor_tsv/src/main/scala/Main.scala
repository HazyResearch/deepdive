package org.deepdive.udf.nlp

import scala.io.Source
import java.util.Properties

object Main extends App {

  // Parse command line options
  case class Config(maxSentenceLength: String)

  val parser = new scopt.OptionParser[Config]("run.sh") {
    head("DocumentParser for TSV Extractors", "0.1")
    head("Input: a TSV file. The first row is document_id, the second row is the content of document.")
    opt[String]('l', "maxLength") action { (x, c) =>
      c.copy(maxSentenceLength = x) 
    } text("Maximum length of sentences to parse (makes things faster) (default: 40)")
  }

  val conf = parser.parse(args, Config("40")) getOrElse { 
    throw new IllegalArgumentException
  }

  System.err.println(s"Parsing with max_len=${conf.maxSentenceLength}")

  // Configuration has been parsed, execute the Document parser
  val props = new Properties()
  props.put("annotators", "tokenize, cleanxml, ssplit, pos, lemma, ner, parse")
  props.put("parse.maxlen", conf.maxSentenceLength)
  props.put("threads", "1") // Should use extractor-level parallelism
  val dp = new DocumentParser(props)

  // Read each json object from stdin and parse the document
  Source.stdin.getLines.zipWithIndex.foreach { case(line, idx) =>

    val tsvArr = line.trim.split("\t")
    if (tsvArr.length >= 2) // skip malformed lines
    {
      val documentId = tsvArr(0)
      val documentStr = tsvArr(1)
  
      System.err.println(s"Parsing document ${documentId}...")
  
      // Output a TSV row for each sentence
      dp.parseDocumentString(documentStr).sentences.zipWithIndex
          .foreach { case (sentenceResult, sentence_offset) =>
  
        if (documentId != "") 
          Console.println(List(
            documentId,
            sentenceResult.sentence,
            dp.list2TSVArray(sentenceResult.words),
            dp.list2TSVArray(sentenceResult.lemma),
            dp.list2TSVArray(sentenceResult.wordsWithPos),
            dp.list2TSVArray(sentenceResult.deps),
            dp.list2TSVArray(sentenceResult.nerTags),
            sentence_offset.toString,
            s"${documentId}@${sentence_offset}" // sentence_id
          ).mkString("\t"))
      }  
    } else {
      System.err.println(s"Warning: skipped malformed line ${idx}: ${line}")
    }
  }

}
