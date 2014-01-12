#! /usr/bin/env python

import fileinput
import json
import sys

# For each tuple..
for line in fileinput.input():
  # From: titles(id, title, has_extractions)
  # To: words(id, title_id, word)
  row = json.loads(line)
  # We are emitting one variable and one factor for each word.
  sys.stderr.write(json.dumps({"title": row[0], "has_extractions": bool(row[1])}) + "\n")
  has_extractions = bool(row[1]) if row[1] else None 
  print json.dumps({"title": row[0], "has_extractions": has_extractions})