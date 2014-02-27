#! /usr/bin/env python

import fileinput
import json

# For each tuple..
for line in fileinput.input():
  # From: titles(id, title, has_extractions)
  # To: words(id, title_id, word)
  row = json.loads(line)
  # We are emitting one variable and one factor for each word.
  if row["title"] is not None:
    # print json.dumps(list(set(title.split(" "))))
    for word in set(row["title"].split(" ")):
      # (title_id, word) - The id is automatically assigned.
      print json.dumps({"title_id": int(row["id"]), "word": word, "is_present": True})