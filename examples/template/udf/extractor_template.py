#! /usr/bin/env python

import json, sys
import ddlib  # Load the ddlib Python library for NLP functions

# For each input row
for line in sys.stdin:
  # Load the JSON object
  row = json.loads(line)
  
  # Output data
  print json.dumps(row)
