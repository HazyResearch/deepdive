#! /usr/bin/env python

import fileinput

ARR_DELIM = '~^~'

# For each input tuple
for row in fileinput.input():
  parts = row.strip().split('\t')
  if len(parts) != 6: 
    print >>sys.stderr, 'Failed to parse row:', row
    continue
  
  # Get all fields from a row
  words = parts[0].split(ARR_DELIM)
  relation_id = parts[1]
  p1_start, p1_length, p2_start, p2_length = [int(x) for x in parts[2:]]
  p1_end = p1_start + p1_length
  p2_end = p2_start + p2_length

  p1_text = words[p1_start:p1_length]
  p2_text = words[p2_start:p2_length]

  # Features for this pair come in here
  features = set()
  
  # Feature 1: Words between the two phrases
  left_idx = min(p1_end, p2_end)
  right_idx = max(p1_start, p2_start)
  words_between = words[left_idx:right_idx]
  if words_between: 
    features.add("words_between=" + "-".join(words_between))

  # Feature 2: Number of words between the two phrases
  features.add("num_words_between=%s" % len(words_between))

  # Feature 3: Does the last word (last name) match assuming the words are not equal?
  last_word_left = words[p1_end-1]
  last_word_right = words[p2_end-1]
  if (last_word_left == last_word_right) and (p1_text != p2_text):
    features.add("last_word_matches")

  # TODO: Add more features, look at dependency paths, etc


  for feature in features:  
    print str(relation_id) + '\t' + feature
