#! /usr/bin/env python
# File: udf/ext_has_spouse_features.py

import sys, json

# For each input tuple
for row in sys.stdin:
  obj = json.loads(row)

  # Get useful data from the JSON
  p1_start = obj["p1.start_position"]
  p1_length = obj["p1.length"]
  p1_end = p1_start + p1_length
  p2_start = obj["p2.start_position"]
  p2_length = obj["p2.length"]
  p2_end = p2_start + p2_length

  p1_text = obj["words"][p1_start:p1_length]
  p2_text = obj["words"][p2_start:p2_length]

  # Features for this pair come in here
  features = set()
  
  # Feature 1: Bag of words between the two phrases
  left_idx = min(p1_end, p2_end)
  right_idx = max(p1_start, p2_start)
  words_between = obj["words"][left_idx:right_idx]
  if words_between: 
    # features.add("words_between=" + "-".join(words_between))
    for word in words_between:
      features.add("word_between=" + word)

  # Feature 2: Number of words between the two phrases
  features.add("num_words_between=%s" % len(words_between))

  # Feature 3: Does the last word (last name) match assuming the words are not equal?
  last_word_left = obj["words"][p1_end - 1]
  last_word_right = obj["words"][p2_end - 1]
  if (last_word_left == last_word_right) and (p1_text != p2_text):
    features.add("last_word_matches")

  # TODO: Add more features, look at dependency paths, etc

  for feature in features:  
    print json.dumps({
      "relation_id": obj["relation_id"],
      "feature": feature
    })