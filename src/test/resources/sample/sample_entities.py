#! /usr/bin/env python

import fileinput
import json

for line in fileinput.input():
  data = json.loads(line)
  if data[3][0].isupper():
    print json.dumps([int(data[0]), data[3], None])