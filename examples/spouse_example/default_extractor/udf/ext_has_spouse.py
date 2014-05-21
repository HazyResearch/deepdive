#! /usr/bin/env python
# File: udf/ext_has_spouse.py

# Sample input data (piped into STDIN):
'''
{"p1.text":"Kaplan","p1.mention_id":84,"p2.text":"Charles Foster Kane","p2.mention_id":62,"sentence_id":44314}
{"p1.text":"Charles Foster Kane","p1.mention_id":62,"p2.text":"Kaplan","p2.mention_id":84,"sentence_id":44314}
{"p1.text":"Robinson","p1.mention_id":575,"p2.text":"Barack Obama","p2.mention_id":549,"sentence_id":45498}
'''

import json, csv, os, sys
BASE_DIR = os.path.dirname(os.path.realpath(__file__))

# Load the spouse dictionary for distant supervision
spouses = {}
with open (BASE_DIR + "/../../data/spouses.csv") as csvfile:
  reader = csv.reader(csvfile)
  for line in reader:
    name1 = line[0].strip().lower()
    name2 = line[1].strip().lower()
    spouses[name1] = name2
    spouses[name2] = name1

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
  p1_text = obj["p1.text"].strip()
  p2_text = obj["p2.text"].strip()
  p1_lower = p1_text.lower()
  p2_lower = p2_text.lower()

  is_true = None
  if p1_lower in spouses and spouses[p1_lower] == p2_lower: 
    is_true = True    # the mention pair is in our supervision dictionary
  elif (p1_lower, p2_lower) in non_spouses: 
    is_true = False   # they appear in other relations
  elif (p1_text == p2_text) or (p1_text in p2_text) or (p2_text in p1_text):
    is_true = False   # they are the same person

  print json.dumps({"person1_id": obj["p1.mention_id"],  "person2_id": obj["p2.mention_id"],
    "sentence_id": obj["sentence_id"],  "description": "%s-%s" %(p1_text, p2_text),
    "is_true": is_true,  "relation_id": None,  "id": None
  })