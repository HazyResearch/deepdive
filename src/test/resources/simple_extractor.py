#! /usr/bin/env python

import fileinput
import json

for line in fileinput.input():
  _id, docid, text = json.loads(line)
  print json.dumps([docid])