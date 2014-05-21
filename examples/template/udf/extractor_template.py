#! /usr/bin/env python

import fileinput
import json
import sys
import os

# Load the ddlib Python library for NLP functions
sys.path.append("%s/ddlib" % os.environ["DEEPDIVE_HOME"])
import lib.dd

# For each input row
for line in fileinput.input():
  # Load the JSON object
  row = json.loads(line)
  
  # Output data
  print json.dumps(row)