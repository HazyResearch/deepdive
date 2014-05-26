#! /usr/bin/env python

import ddext
import itertools
# Format of plpy_extractor: 
# Anything Write functions "init", "run" will not be accepted.
# In "init", import libraries, specify input variables and return types
# In "run", write your extractor. Return a list containing your results, each item in the list should be a list/tuple of your return types.
# Do not print.

def init():
  # SD['json'] = __import__('json')
  ddext.import_lib('itertools')
  
  # Input commands MUST HAVE CORRECT ORDER
  ddext.input('sentence_id', 'text')
  ddext.input('words', 'text[]')
  ddext.input('ner_tags', 'text[]')

  # Returns commands MUST HAVE CORRECT ORDER
  ddext.returns('sentence_id', 'text')
  ddext.returns('start_position', 'int')
  # ddext.returns('start_index', 'int')
  ddext.returns('length', 'int')
  ddext.returns('text', 'text')
  ddext.returns('mention_id', 'text')
  

def run(sentence_id, words, ner_tags):
  
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

  # You can yield a tuple or a dict to database
  for phrase in phrases_indicies:
    # yield (sentence_id, 
    #     phrase[0], 
    #     len(phrase), 
    #     " ".join(words[phrase[0]:phrase[-1]+1]))
    yield {
        'sentence_id': sentence_id, 
        'start_position': phrase[0], 
        'text': " ".join(words[phrase[0]:phrase[-1]+1]),
        'length': len(phrase),
        "mention_id": '%s_%d' % (sentence_id, phrase[0])
        }

  # # Or you can return a list of tuples
  # return [(sentence_id, phrase[0], len(phrase),
  #         " ".join(words[phrase[0]:phrase[-1]+1])) for phrase in phrases_indicies]
