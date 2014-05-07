#! /usr/bin/env python

import ddext

# Format of plpy_extractor: 
# Anything Write functions "init", "run" will not be accepted.
# In "init", import libraries, specify input variables and return types
# In "run", write your extractor. Return a list containing your results, each item in the list should be a list/tuple of your return types.
# Do not print.

def init():
  # SD['json'] = __import__('json')
  ddext.input('words', 'text[]')
  ddext.input('relation_id', 'bigint') # Orders of input MATTERS now
  ddext.input('p1_start', 'int')
  ddext.input('p1_length', 'int')
  ddext.input('p2_start', 'int')
  ddext.input('p2_length', 'int')

  ddext.returns('relation_id', 'bigint')
  ddext.returns('feature', 'text')

def run(words, relation_id, p1_start, p1_length, p2_start, p2_length):
  # This function will be run for each input tuple
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

  for feature in features:
    yield relation_id, feature
  # return [(relation_id, feature) for feature in features]