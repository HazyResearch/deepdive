#! /usr/bin/env python

import json, csv, os, sys
from collections import defaultdict

BASE_DIR = os.path.dirname(os.path.realpath(__file__))

# Load the spouse dictionary for distant supervision
spouses = {}
with open (BASE_DIR + "/../../data/spouses.csv") as csvfile:
  reader = csv.reader(csvfile)
  for line in reader:
    name1 = line[0].strip().lower()
    name2 = line[1].strip().lower()
    spouses[name1] = name2

# Load relations of people that are not spouse
non_spouses = set()
lines = open(BASE_DIR + '/../../data/non-spouses.tsv').readlines()
for line in lines:
  name1, name2, relation = line.strip().split('\t')
  non_spouses.add((name1, name2))  # Add a non-spouse relation pair

# For each input tuple
for row in sys.stdin:
  obj = json.loads(row)

  # Get useful data from the JSON
  p1_id = obj["p1.mention_id"]
  p1_text = obj["p1.text"].strip()
  p1_lower = p1_text.lower()
  p2_id = obj["p2.mention_id"]
  p2_text = obj["p2.text"].strip()
  p2_lower = p2_text.lower()
  sentence_id = obj["sentence_id"]

  # See if the combination of people is in our supervision dictionary
  # If so, set is_correct to true or false
  is_true = None
  if p1_lower in spouses and spouses[p1_lower] == p2_lower:
    is_true = True
  if p2_lower in spouses and spouses[p2_lower] == p1_lower:
    is_true = True
  # appear in other relations
  elif (p1_lower, p2_lower) in non_spouses: 
    is_true = False
  elif (p2_lower, p1_lower) in non_spouses:
    is_true = False
  # same person
  elif (p1_text == p2_text) or (p1_text in p2_text) or (p2_text in p1_text):
    is_true = False

  print json.dumps({
    "person1_id": p1_id,
    "person2_id": p2_id,
    "sentence_id": sentence_id,
    "description": "%s-%s" %(p1_text, p2_text),
    "is_true": is_true,
    "relation_id": None,
    "id": None
  })