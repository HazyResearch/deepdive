#! /usr/bin/env python

import fileinput
import json

# For each input tuple
for row in fileinput.input():
  obj = json.loads(row)

  # Get useful data from the JSON
  p1_start = obj["people_mentions.p1.start_position"]
  p1_length = obj["people_mentions.p1.length"]
  p1_end = p1_start + p1_length
  p2_start = obj["people_mentions.p2.start_position"]
  p2_length = obj["people_mentions.p2.length"]
  p2_end = p2_start + p2_length

  p1_text = obj["sentences.words"][p1_start:p1_length]
  p2_text = obj["sentences.words"][p2_start:p2_length]

  # Features for this pair come in here
  features = set()
  
  # Feature 1: Words between the two phrases
  left_idx = min(p1_end, p2_end)
  right_idx = max(p1_start, p2_start)
  words_between = obj["sentences.words"][left_idx:right_idx]
  if words_between: 
    features.add("words_between=" + "-".join(words_between))
    features.add("words_between_bag=" + "-".join(sorted(set(words_between))))

  # Feature 2: Number of words between the two phrases
  features.add("num_words_between=%s" % len(words_between))

  # Feature 4: POS tags of the two words
  left_pos_tags = "-".join(obj["sentences.pos_tags"][p1_start:p1_end])
  right_pos_tags = "-".join(obj["sentences.pos_tags"][p2_start:p2_end])
  features.add("pos_tags=(%s,%s)" %(left_pos_tags, right_pos_tags))

  # Feature 5: Does the last word (last name) match assuming the words are not equal?
  last_word_left = obj["sentences.words"][p1_end-1]
  last_word_right = obj["sentences.words"][p2_end-1]
  if (last_word_left == last_word_right) and (p1_text != p2_text):
    features.add("last_word_matches")


  # TODO: Add more features, look at dependency paths, etc

  for feature in features:  
    print json.dumps({
      "relation_id": obj["has_spouse.id"],
      "feature": feature
    })