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
  ddext.input('ner_tags', 'text[]')
  ddext.input('lemma', 'text[]')
  ddext.input('relation_id', 'bigint') # Orders of input MATTERS now
  ddext.input('p1_start', 'int')
  ddext.input('p1_length', 'int')
  ddext.input('p2_start', 'int')
  ddext.input('p2_length', 'int')


  ddext.returns('relation_id', 'bigint')
  ddext.returns('feature', 'text')

def run(words, ner_tags, lemma, relation_id, p1_start, p1_length, p2_start, p2_length):
  # This function will be run for each input tuple
  p1_end = p1_start + p1_length
  p2_end = p2_start + p2_length

  p1_text = words[p1_start:p1_length]
  p2_text = words[p2_start:p2_length]

  # Features for this pair come in here
  features = set()

  ######################## 
  # Improved Feature Set #
  ########################
  
  # Feature 1: Find out if a lemma of marry occurs.
  # A better feature would ensure this is on the dependency path between the two.
  left_idx = min(p1_end, p2_end)
  right_idx = max(p1_start, p2_start)
  words_between = words[left_idx:right_idx]
  lemma_between = lemma[left_idx:right_idx]
  married_words = ['marry', 'widow', 'wife', 'fiancee', 'spouse']
  non_married_words = ['father', 'mother', 'brother', 'sister', 'son']
  if len(words_between) <= 10:
    for mw in married_words + non_married_words:
      if mw in lemma_between: 
        features.add("important_word=%s" % mw)


  # # Feature 1: Bag of words between the two phrases
  # left_idx = min(p1_end, p2_end)
  # right_idx = max(p1_start, p2_start)
  # words_between = words[left_idx:right_idx]
  # if words_between:
  #   for word in words_between:
  #     features.add("word_between=" + word)

  # Feature 2: Number of words between the two phrases
  # Intuition: if they are close by, the link may be stronger.
  l = len(words_between)
  if l < 5: features.add("few_words_between")
  else: features.add("many_words_between")

  # Feature 3: Does the last word (last name) match assuming the words are not equal?
  last_word_left = words[p1_end-1]
  last_word_right = words[p2_end-1]
  if (last_word_left == last_word_right) and (p1_text != p2_text):
    features.add("last_word_matches")


  # # Feature 4: Other person between this mention
  # if 'PERSON' in ner_tags[left_idx:right_idx]:
  #   features.add("other_person_between")

  for feature in features:
    yield relation_id, feature
  # return [(relation_id, feature) for feature in features]
