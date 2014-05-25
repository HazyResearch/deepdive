#! /usr/bin/env python
# File: udf/ext_people.py

# Sample input data (piped into STDIN):
'''
{"sentence_id":"118238@10","words":["Sen.","Barack","Obama","and","his","wife",",","Michelle","Obama",",","have","released","eight","years","of","joint","returns","."],"ner_tags":["O","PERSON","PERSON","O","O","O","O","PERSON","PERSON","O","O","O","DURATION","DURATION","O","O","O","O"]}
{"sentence_id":"118238@12","words":["During","the","2004","presidential","campaign",",","we","urged","Teresa","Heinz","Kerry",",","the","wealthy","wife","of","Sen.","John","Kerry",",","to","release","her","tax","returns","."],"ner_tags":["O","O","DATE","O","O","O","O","O","PERSON","PERSON","PERSON","O","O","O","O","O","O","PERSON","PERSON","O","O","O","O","O","O","O"]}
'''

import json, sys

# For-loop for each row in the input query
for row in sys.stdin:
  # load JSON format of the current tuple
  sentence_obj = json.loads(row)
  # Find phrases that are continuous words tagged with PERSON.
  phrases = []   # Store (start_position, length, text)
  words = sentence_obj["words"]         # a list of all words
  ner_tags = sentence_obj["ner_tags"]   # ner_tags for each word
  start_index = 0
  
  while start_index < len(words):
    # Checking if there is a PERSON phrase starting from start_index
    index = start_index
    while index < len(words) and ner_tags[index] == "PERSON": 
      index += 1
    if index != start_index:   # found a person from "start_index" to "index"
      text = ' '.join(words[start_index:index])
      length = index - start_index
      phrases.append((start_index, length, text))
    start_index = index + 1
  
  # Output a tuple for each PERSON phrase
  for start_position, length, text in phrases:
    print json.dumps({
      "sentence_id": sentence_obj["sentence_id"],
      "start_position": start_position,
      "length": length,
      "text": text,
      # Create an unique mention_id by sentence_id + offset:
      "mention_id": '%s_%d' % (sentence_obj["sentence_id"], start_position)
    })
