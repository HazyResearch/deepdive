#! /usr/bin/env python

import csv, os, sys

# The directory of this UDF file
BASE_DIR = os.path.dirname(os.path.realpath(__file__))

# Load the spouse dictionary for distant supervision.
# A person can have multiple spouses
spouses = set()
married_people = set()
lines = open(BASE_DIR + '/../data/spouses.tsv').readlines()
for line in lines:
  name1, name2, relation = line.strip().split('\t')
  spouses.add((name1, name2))  # Add a spouse relation pair
  married_people.add(name1)    # Record the person as married
  married_people.add(name2)

# Load relations of people that are not spouse
# The non-spouse KB lists incompatible relations, e.g. childrens, siblings, parents.
non_spouses = set()
lines = open(BASE_DIR + '/../data/non-spouses.tsv').readlines()
for line in lines:
  name1, name2, relation = line.strip().split('\t')
  non_spouses.add((name1, name2))  # Add a non-spouse relation pair

# For each input tuple
for row in sys.stdin:
  parts = row.strip().split('\t')
  sentence_id, p1_id, p1_text, p2_id, p2_text = parts

  p1_text = p1_text.strip()
  p2_text = p2_text.strip()
  p1_text_lower = p1_text.lower()
  p2_text_lower = p2_text.lower()

  # DS rule 1: true if they appear in spouse KB, false if they appear in non-spouse KB
  is_true = '\N'
  if (p1_text_lower, p2_text_lower) in spouses or \
     (p2_text_lower, p1_text_lower) in spouses:
    is_true = '1'
  elif (p1_text_lower, p2_text_lower) in non_spouses or \
       (p2_text_lower, p1_text_lower) in non_spouses:
    is_true = '0'
  # DS rule 3: false if they appear to be in same person
  elif (p1_text == p2_text) or (p1_text in p2_text) or (p2_text in p1_text):
    is_true = '0'
  # DS rule 4 false if they are both married, but not married to each other:
  elif p1_text_lower in married_people and p2_text_lower in married_people:
    is_true = '0'

  # Output relation candidates into output table
  print '\t'.join([
    p1_id, p2_id, sentence_id, 
    "%s-%s" %(p1_text, p2_text),
    is_true,
    "%s-%s" %(p1_id, p2_id),
    '\N'   # leave "id" blank for system!
    ])

  # TABLE FORMAT: CREATE TABLE has_spouse(
  # person1_id bigint,
  # person2_id bigint,
  # sentence_id bigint,
  # description text,
  # is_true boolean,
  # relation_id bigint, -- unique identifier for has_spouse
  # id bigint   -- reserved for DeepDive
  # );
