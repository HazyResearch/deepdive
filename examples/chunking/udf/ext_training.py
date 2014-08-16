#! /usr/bin/env python

# extract training data

import fileinput
import json
import itertools
import tags
import sys

tagNames = ['NP', 'VP', 'PP', 'ADJP', 'ADVP', 'SBAR', 'O', 'PRT', 'CONJP', 'INTJ', 'LST', 'B', '']
sentID = 1

# for each word
for row in fileinput.input():
	obj = json.loads(row)

	# get tag
	# extractor bug!
	if 'tag' in obj.keys():
		tag = obj['tag']
		if (tag != None and tag != 'O'): 
			tag = tag.split('-')[1]

		if tag not in tagNames: 
			tag = ''

		print json.dumps({
			'sent_id' : sentID,
			'word_id'	: obj['word_id'],
			'word'		: obj['word'],
			'pos'			: obj['pos'],
			'true_tag': obj['tag'],
			'tag'			: tagNames.index(tag)
		})

	else:
		sentID += 1
		print json.dumps({
			'sent_id' : None,
			'word_id'	: obj['word_id'],
			'word'		: None,
			'pos'			: None,
			'true_tag': None,
			'tag'			: tagNames.index('')
		})