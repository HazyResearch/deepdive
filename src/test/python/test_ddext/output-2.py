DROP TYPE IF EXISTS ret_func_ext_has_spouse_candidates CASCADE;
CREATE TYPE ret_func_ext_has_spouse_candidates AS (person1_id text, person2_id text, sentence_id text, description text, is_true boolean, relation_id text);
CREATE OR REPLACE FUNCTION func_ext_has_spouse_candidates(
    sentence_id text, p1_id text, p1_text text, p2_id text, p2_text text) RETURNS SETOF ret_func_ext_has_spouse_candidates AS
$$

if 'csv' in SD:
  csv = SD['csv']
else:
  import csv
  SD['csv'] = csv

if 'os' in SD:
  os = SD['os']
else:
  import os
  SD['os'] = os

if 'defaultdict' in SD:
  defaultdict = SD['defaultdict']
else:
  from collections import defaultdict
  SD['defaultdict'] = defaultdict


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
$$ LANGUAGE plpythonu ;
