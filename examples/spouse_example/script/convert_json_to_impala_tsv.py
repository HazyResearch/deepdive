#!/usr/bin/env python3

import json
import sys

filesource = sys.argv[1]

#filesource = '../data/sentences_dump.json'
with open(filesource) as f:
    for line in f:
        # don't allow tabs
        #print(line, file=sys.stderr)
        s = str(line).replace("\t", " ")
        if s.rstrip() == "":
            continue
        o = json.loads(s)
        if not 'document_id' in o:
            continue
        #print(o['document_id'], file=sys.stderr)
        print(str(o['document_id']) + 
	  '\t' + o['sentence'] + 
	  '\t' + '~^~'.join(o['words']) + 
	  '\t' + '~^~'.join(o['lemma']) + 
	  '\t' + '~^~'.join(o['pos_tags']) + 
          '\t' + '~^~'.join(o['dependencies']) + 
          '\t' + '~^~'.join(o['ner_tags']) +
          '\t' + str(o['sentence_offset']) +
	  '\t' + o['sentence_id']) 

