#! /usr/bin/env python
# File: udf/ext_has_spouse.py

# Sample input data (piped into STDIN):
'''
{"p1_text":"Michelle Obama","p1_mention_id":"118238@10_7","p2_text":"Barack Obama","p2_mention_id":"118238@10_1","sentence_id":"118238@10"}
{"p1_text":"Barack Obama","p1_mention_id":"118238@10_1","p2_text":"Michelle Obama","p2_mention_id":"118238@10_7","sentence_id":"118238@10"}
{"p1_text":"John Kerry","p1_mention_id":"118238@12_17","p2_text":"Teresa Heinz Kerry","p2_mention_id":"118238@12_8","sentence_id":"118238@12"}'''

import json, csv, os, sys
BASE_DIR = os.path.dirname(os.path.realpath(__file__))

# Load the spouse dictionary for distant supervision
spouses = set()  # One person may have multiple spouses, so use set
with open (BASE_DIR + "/../../data/spouses.csv") as csvfile:
  reader = csv.reader(csvfile)
  for line in reader:
    name1 = line[0].strip().lower()
    name2 = line[1].strip().lower()
    spouses.add((name1, name2))
    spouses.add((name2, name1))

# Load relations of people that are not spouse
non_spouses = set()
lines = open(BASE_DIR + '/../../data/non-spouses.tsv').readlines()
for line in lines:
  name1, name2, relation = line.strip().split('\t')
  non_spouses.add((name1, name2))  # Add a non-spouse relation pair
  non_spouses.add((name2, name1))

# For each input tuple
for row in sys.stdin:
  obj = json.loads(row)
  p1_text = obj["p1_text"].strip()
  p2_text = obj["p2_text"].strip()
  p1_lower = p1_text.lower()
  p2_lower = p2_text.lower()

  is_true = None
  if (p1_lower, p2_lower) in spouses:
    is_true = True    # the mention pair is in our supervision dictionary
  elif (p1_lower, p2_lower) in non_spouses: 
    is_true = False   # they appear in other relations
  elif (p1_text == p2_text) or (p1_text in p2_text) or (p2_text in p1_text):
    is_true = False   # they are the same person

  # Create an unique relation_id by a combination of two mention's IDs
  relation_id = obj["p1_mention_id"] + '_' + obj["p2_mention_id"]

  print json.dumps({"person1_id": obj["p1_mention_id"],  "person2_id": obj["p2_mention_id"],
    "sentence_id": obj["sentence_id"],  "description": "%s-%s" %(p1_text, p2_text),
    "is_true": is_true,  "relation_id": relation_id,  "id": None
  })