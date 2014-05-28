#! /usr/bin/env python

import unittest
import ddlib as dd

class TestDDLib(unittest.TestCase):
  
  def setUp(self):
    self.words = ["Tanja", "married", "Jake", "five", "years", "ago"]
    self.lemma = ["Tanja", "marry", "Jake", "five", "years", "ago"]

  def test_materialize_span(self):
    span1 = dd.Span(0, 3)
    materialized_span = dd.materialize_span(self.words, span1)
    self.assertEqual(materialized_span[:], ["Tanja", "married", "Jake"])

  def test_tokens_between_spans(self):
    span1 = dd.Span(0, 2)
    span2 = dd.Span(3, 5)
    words_between = dd.tokens_between_spans(self.words, span1, span2)
    self.assertEqual(words_between[:], (False, ["Jake"]))
    words_between = dd.tokens_between_spans(self.words, span2, span1)
    self.assertEqual(words_between[:], (True, ["Jake"]))
    words_between = dd.tokens_between_spans(self.words, span1, span1)
    self.assertEqual(words_between[:], (False, []))


if __name__ == '__main__':
  unittest.main()
