package org.deepdive.udf.nlp

case class SentenceParseResult(sentence: String, words: List[String], lemma: List[String], 
  wordsWithPos: List[String], depLabels: List[String], depParents: List[Int], nerTags: List[String])
case class DocumentParseResult(sentences: List[SentenceParseResult])