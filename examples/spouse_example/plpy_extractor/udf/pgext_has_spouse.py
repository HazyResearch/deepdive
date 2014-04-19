#! /usr/bin/env python
#! /usr/bin/env python

import ddext
from ddext import SD
import itertools

# input: sentences.id, p1.id, p1.text, p2.id, p2.text
# output: has_spouse
# returns: 

def init():
  # SD['json'] = __import__('json')
  ddext.import_lib('csv')
  ddext.import_lib('os')
  
  # Input commands MUST HAVE CORRECT ORDER: 
  # SAME AS SELECT ORDER, and SAME AS "run" ARGUMENT ORDER
  ddext.input('sentence_id', 'bigint')
  ddext.input('p1_id', 'bigint')
  ddext.input('p1_text', 'text[]')
  ddext.input('p2_id', 'bigint')
  ddext.input('p2_text', 'text[]')

  # Returns commands MUST HAVE CORRECT ORDER
  ddext.returns('person1_id', 'bigint')
  ddext.returns('person2_id', 'bigint')
  ddext.returns('sentence_id', 'bigint')
  ddext.returns('description', 'text')
  ddext.returns('is_true', 'boolean')


def run(sentence_id, p1_id, p1_text, p2_id, p2_text):
  
  ####### NOTICE: SHARED MEMORY ########
  # If you really need shared memory, do "from ddext import SD."
  # Use SD as the global shared dict.

  # Load the spouse dictionary for distant supervision
  spouses = defaultdict(lambda: None)
  if 'spouses' in SD:
    spouses = SD['spouses']
  else:
    SD['spouses'] = spouses

  # Read dict from file: MAKE SURE YOUR DATABASE SERVER 
  #   HAVE THE ACCESS TO FILE!!
  # e.g. put this file in afs/dfs, and properly name the path.
  with open (BASE_DIR + "/../../data/spouses.csv") as csvfile:
    reader = csv.reader(csvfile)
    for line in reader:
      spouses[line[0].strip().lower()] = line[1].strip().lower()


  p1_text = p1_text.strip()
  p2_text = p2_text.strip()
  p1_text_lower = p1_text.lower()
  p2_text_lower = p2_text.lower()

  is_true = None
  if spouses[p1_text_lower] == p2_text_lower:
    is_true = True
  elif (p1_text == p2_text) or (p1_text in p2_text) or (p2_text in p1_text):
    is_true = False

  return [p1_id, p2_id, sentence_id, "%s-%s" % (p1_text, p2_text), is_true]
