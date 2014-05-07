#! /usr/bin/env python

import fileinput
import json
import csv
import os
import sys
from collections import defaultdict

BASE_DIR = os.path.dirname(os.path.realpath(__file__))

# Load the spouse dictionary for distant supervision
spouses = defaultdict(lambda: None)
with open (BASE_DIR + "/../../data/spouses.csv") as csvfile:
  reader = csv.reader(csvfile)
  for line in reader:
    spouses[line[0].strip().lower()] = line[1].strip().lower()


# For each input tuple
for row in fileinput.input():
  obj = json.loads(row)

  # Get useful data from the JSON
  p1_id = obj["p1.mention_id"]
  p1_text = obj["p1.text"].strip()
  p1_text_lower = p1_text.lower()
  p2_id = obj["p2.mention_id"]
  p2_text = obj["p2.text"].strip()
  p2_text_lower = p2_text.lower()
  sentence_id = obj["sentence_id"]

  # See if the combination of people is in our supervision dictionary
  # If so, set is_correct to true or false
  is_true = None
  if spouses[p1_text_lower] == p2_text_lower:
    is_true = True
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