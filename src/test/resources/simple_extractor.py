#! /usr/bin/env python

import fileinput
import json

for line in fileinput.input():
  row = json.loads(line)
  print json.dumps({"document_id" : row["documents.docid"]})