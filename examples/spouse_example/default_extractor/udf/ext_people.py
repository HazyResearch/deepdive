#! /usr/bin/env python

import fileinput
import json
import itertools

# For each sentence
for row in fileinput.input():
  sentence_obj = json.loads(row)

  # Find phrases that are tagged with PERSON
  phrases_indicies = []
  start_index = 0
  ner_list = list(enumerate(sentence_obj["ner_tags"]))
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
    print json.dumps({
      "sentence_id": sentence_obj["sentence_id"],
      "start_position": phrase[0],
      "length": len(phrase),
      "text": " ".join(sentence_obj["words"][phrase[0]:phrase[-1]+1]),
      "mention_id": None
    })
