#! /usr/bin/env python
# File: udf/ext_has_spouse_features.py

import sys, json
import ddlib

# For each input tuple
# TODO: Sample Data and the input schema. 
# sample json
for row in sys.stdin:

  # Unpack input into tuples.
  #
  obj = json.loads(row)
  words, lemmas = obj["words"], obj["lemma"]
  span1 = ddlib.Span(begin_word_id=obj['p1.start_position'], length=obj['p1.length'])
  span2 = ddlib.Span(begin_word_id=obj['p2.start_position'], length=obj['p2.length'])

  features = set()

  # Feature 1: Find out if a lemma of marry occurs.
  # A better feature would ensure this is on the dependency path between the two.
  #
  lemma_between = ddlib.tokens_between_spans(lemmas, span1, span2)
  married_words = ('marry', 'widow')
  for lemma in lemma_between.elements:
    if lemma in married_words:
      features.add("important_word=%s" % lemma) 

  # Feature 2: The number of words between the two phrases.
  # Intuition: if they are close by, the link may be stronger.
  #
  words_between = ddlib.tokens_between_spans(words, span1, span2)
  l = len(words_between.elements)
  features.add("num_words_between=%s" % l if l<5 else "many_words_between")

  # Feature 3: Check if the last name matches heuristically.
  # 
  last_word_left = ddlib.materialize_span(words, span1)[-1]
  last_word_right = ddlib.materialize_span(words, span2)[-1]
  if (last_word_left == last_word_right): 
    features.add("potential_last_name_match")

  # Use this line if you want to print out all features extracted
  #
  #ddlib.log(features)

  for feature in features:  
    print json.dumps({
      "relation_id": obj["relation_id"],
      "feature": feature
    })

