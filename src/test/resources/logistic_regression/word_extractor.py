#! /usr/bin/env python

import fileinput
import json

for line in fileinput.input():
  row = json.loads(line)
  # From: words{ id: Integer, sentence_id: Integer, position: Integer, text: Text }
  # To: entities{ id: Integer, word_id: Integer, text: Text, is_evidence: Boolean }
  if row["words.text"][0].isupper():
    print json.dumps({"word_id" : int(row["words.id"]), "text": row["words.text"], "is_evidence": None})