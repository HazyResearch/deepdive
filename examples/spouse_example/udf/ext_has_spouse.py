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
with open (BASE_DIR + "/../data/spouses.csv") as csvfile:
  reader = csv.reader(csvfile)
  for line in reader:
    spouses[line[0].strip().lower()] = line[1].strip().lower()


# For each input tuple
for row in fileinput.input():
  obj = json.loads(row)

  # Get useful data from the JSON
  p1_id = obj["people_mentions.p1.id"]
  p1_text = obj["people_mentions.p1.text"]
  p2_id = obj["people_mentions.p2.id"]
  p2_text = obj["people_mentions.p2.text"]
  sentence_id = obj["sentences.id"]

  # See if the combination of people is in our supervision dictionary
  # If so, set is_correct to true or false
  is_true = None
  if spouses[p1_text.strip().lower()] == p2_text.strip().lower():
    is_true = True
  if p1_text.strip().lower() == p2_text.strip().lower():
    is_true = False

  print json.dumps({
    "person1_id": p1_id,
    "person2_id": p2_id,
    "sentence_id": sentence_id,
    "description": "%s-%s" %(p1_text, p2_text),
    "is_true": is_true
  })