#! /usr/bin/env python

import fileinput
import itertools

ARR_DELIM = '~^~'

# For each sentence
for row in fileinput.input():
  
  sentence_id, words_str, ner_tags_str = row.split('\t')
  words = words_str.split(ARR_DELIM)
  ner_tags = ner_tags_str.split(ARR_DELIM)

  # Find phrases that are tagged with PERSON
  phrases_indicies = []
  start_index = 0
  ner_list = list(enumerate(ner_tags))
  while True:
    sublist = ner_list[start_index:]
    next_phrase = list(itertools.takewhile(lambda x: (x[1] in ["PERSON"]), sublist))
    if next_phrase:
      phrases_indicies.append([x[0] for x in next_phrase])
      start_index = next_phrase[-1][0] + 1
    elif start_index == len(ner_list)+1: break
    else: start_index = start_index + 1

  # Output a tuple for each PERSON phrase
  for phrase in phrases_indicies:
    print '\t'.join(
      [ str(x) for x in [
        sentence_id, 
        phrase[0],   # start_position
        len(phrase), # length
        " ".join(words[phrase[0]:phrase[-1]+1]),  # text
        '%s_%d' % (sentence_id, phrase[0])        # mention_id
      ]])
