#! /usr/bin/env python

import fileinput
import json
import sys

# For each input row
for line in fileinput.input():
  row = json.loads(line)

  sys.stderr.write("Got data:" + str(row) + "\n")
  
  # All fields in HSQLDB are uppercase
  id_field, some_num, some_text = row["ID"], row["SOME_NUM"], row["SOME_TEXT"]

  # Output data
  print json.dumps({
    "SOME_NUM": id_field,
    "SOME_TEXT": "Hello!"
  })