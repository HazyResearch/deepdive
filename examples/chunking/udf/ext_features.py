#! /usr/bin/env python

import fileinput
import json
import itertools
import sys

def tostr(s):
	return '' if s is None else str(s)

# for each word
for row in fileinput.input():
	obj = json.loads(row)

	features = set()
	# sys.stderr.write(str(obj))

	# features
	w1_word = 'word=' + tostr(obj["w1.word"])
	w1_pos = 'pos=' + tostr(obj["w1.pos"])

	if 'w2.word' in obj.keys():
		w2_word = 'prev_word=' + tostr(obj["w2.word"])
		w2_pos = 'prev_pos=' + tostr(obj["w2.pos"])
	else:
		w2_word = 'prev_word='
		w2_pos = 'prev_pos='
	#w3_word = 'next_word=' + tostr(obj["words_raw.w3.word"])
	#w3_pos = 'next_pos=' + tostr(obj["words_raw.w3.pos"])

	features.add(w1_word)
	features.add(w1_pos)
	features.add(w2_pos)

	for f in features:
		print json.dumps({
			"word_id": obj["w1.word_id"],
			'feature': f
		})