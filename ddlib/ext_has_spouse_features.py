#! /usr/bin/env python
# File: udf/ext_has_spouse_features.py

import sys, json

# For each input tuple
# TODO: Sample Data and the input schema. 
# sample json
for row in sys.stdin:
  obj = json.loads(row)
 
  # Library/DSL??? This is a span, it should be an object.
  p1_start  = obj["p1.start_position"]
  p1_length = obj["p1.length"]
  p1_end    = p1_start + p1_length
  p2_start  = obj["p2.start_position"]
  p2_length = obj["p2.length"]
  p2_end    = p2_start + p2_length

  p1_text = obj["words"][p1_start:p1_length]
  p2_text = obj["words"][p2_start:p2_length]

  left_idx      = min(p1_end, p2_end)
  right_idx     = max(p1_start, p2_start)
  
  # Features for this pair come in here
  features = set()

  # Feature 1: Find out if a lemma of marry occurs.
  # A better feature would ensure this is on the dependency path between the two.
  lemma_between = obj["lemma"][left_idx:right_idx]
  married_words = ['marry', 'widow']
  for mw in married_words:
    if mw in lemma_between: 
      features.add("important_word=%s" % mw)

  # Feature 2: The number of words between the two phrases.
  # Intuition: if they are close by, the link may be stronger.
  words_between = obj["words"][left_idx:right_idx]
  l = len(words_between)
  if l < 5: features.add("num_words_between=%s" % l)
  else: features.add("many_words_between")

  # Feature 3: Check if the last name matches heuristically.
  last_word_left  = obj["words"][p1_end - 1]
  last_word_right = obj["words"][p2_end - 1]
  if (last_word_left == last_word_right): 
    features.add("potential_last_name_match")

  # TODO: Add more features, look at dependency paths, etc
  for feature in features:  
    print json.dumps({
      "relation_id": obj["relation_id"],
      "feature": feature
    })
