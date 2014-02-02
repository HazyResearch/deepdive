package org.deepdive.udf.nlp

import edu.stanford.nlp.ling.{CoreLabel, Word, CoreAnnotations}
import edu.stanford.nlp.parser._
import edu.stanford.nlp.parser.lexparser._
import edu.stanford.nlp.process._
import edu.stanford.nlp.trees._
import edu.stanford.nlp.ie.crf._
import edu.stanford.nlp.pipeline._
import edu.stanford.nlp.util._
import edu.stanford.nlp.ling.CoreAnnotations._
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation

import scala.collection.JavaConversions._
import java.io.{StringReader, StringWriter, PrintWriter}
import java.util.Properties


class DocumentParser(props: Properties) {

  val pipeline = new StanfordCoreNLP(props)

  def parseDocumentString(doc: String) = {

    val document = new Annotation(doc)
    pipeline.annotate(document)
    val sentences = document.get(classOf[SentencesAnnotation])

    val sentenceResults = sentences.map { sentence =>
      val tokens = sentence.get(classOf[TokensAnnotation])
      val wordList = tokens.map(_.get(classOf[TextAnnotation]))
      val posList = tokens.map(_.get(classOf[PartOfSpeechAnnotation]))
      val nerList = tokens.map(_.get(classOf[NamedEntityTagAnnotation]))
      val depList = sentence.get(classOf[CollapsedCCProcessedDependenciesAnnotation]).toList.lines
      SentenceParseResult(wordList.mkString(" "), wordList.toList, posList.toList,
        depList.toList, nerList.toList)
    }

    DocumentParseResult(sentenceResults.toList) 
  }



}