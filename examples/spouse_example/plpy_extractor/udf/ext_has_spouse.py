#! /usr/bin/env python
#! /usr/bin/env python

# Imports written in this area is useless, just for local debugging
import ddext
from ddext import SD
import itertools
import os
from collections import defaultdict

# input: sentences.id, p1.id, p1.text, p2.id, p2.text
# output: has_spouse
# returns: 

def init():
  # SD['json'] = __import__('json')
  ddext.import_lib('csv')
  ddext.import_lib('os')
  # from collections import defaultdict
  ddext.import_lib('defaultdict', 'collections')

  # Other examples of import_lib:
  # # "from collections import defaultdict as defdict":
  # ddext.import_lib('defaultdict', 'collections', 'defdict')
  # # "import defaultdict as defdict":
  # ddext.import_lib('defaultdict', as_name='defdict')
  
  # Input commands MUST HAVE CORRECT ORDER: 
  # SAME AS SELECT ORDER, and SAME AS "run" ARGUMENT ORDER
  ddext.input('sentence_id', 'text')
  ddext.input('p1_id', 'text')
  ddext.input('p1_text', 'text')
  ddext.input('p2_id', 'text')
  ddext.input('p2_text', 'text')

  # Returns commands MUST HAVE CORRECT ORDER
  ddext.returns('person1_id', 'text')
  ddext.returns('person2_id', 'text')
  ddext.returns('sentence_id', 'text')
  ddext.returns('description', 'text')
  ddext.returns('is_true', 'boolean')
  ddext.returns('relation_id', 'text')


def run(sentence_id, p1_id, p1_text, p2_id, p2_text):
  
  ####### NOTICE: SHARED MEMORY ########
  # If you really need shared memory / global dir, do "from ddext import SD."
  # Use SD as the global shared dict.

  # Load the spouse dictionary for distant supervision
  spouses = defaultdict(lambda: None)
  if 'spouses' in SD:
    spouses = SD['spouses']
  else:  # Read data from file once, and share it
    SD['spouses'] = spouses
    # Read dict from file: MAKE SURE YOUR DATABASE SERVER 
    #   HAVE THE ACCESS TO FILE!
    # Please use absolute path!
    with open ("/dfs/rulk/0/deepdive/shared/spouses.csv") as csvfile:
      reader = csv.reader(csvfile)
      for line in reader:
        spouses[line[0].strip().lower()] = line[1].strip().lower()

  if 'non_spouses' in SD:
    non_spouses = SD['non_spouses']
  else:
    non_spouses = set()
    SD['non_spouses'] = non_spouses
    lines = open('/dfs/rulk/0/deepdive/shared/non-spouses.tsv').readlines()
    for line in lines:
      name1, name2, relation = line.strip().split('\t')
      non_spouses.add((name1, name2))  # Add a non-spouse relation pair

  # NOTICE: PLPY DOES NOT ALLOW overwritting input arguments!!!
  # will return "UnboundLocalError".
  p1_t = p1_text.strip()
  p2_t = p2_text.strip()
  p1_text_lower = p1_t.lower()
  p2_text_lower = p2_t.lower()

  is_true = None
  if spouses[p1_text_lower] == p2_text_lower:
    is_true = True
  elif spouses[p2_text_lower] == p1_text_lower:
    is_true = True
  # same person
  elif (p1_t == p2_t) or (p1_t in p2_t) or (p2_t in p1_t):
    is_true = False
  elif (p1_text_lower, p2_text_lower) in non_spouses:
    is_true = False
  elif (p2_text_lower, p1_text_lower) in non_spouses:
    is_true = False

  # Must return a tuple of arrays.

  yield [p1_id, p2_id, sentence_id, "%s-%s" % (p1_t, p2_t), is_true, "%s_%s" % (p1_id, p2_id)]
  # return [[p1_id, p2_id, sentence_id, "%s-%s" % (p1_t, p2_t), is_true]]
