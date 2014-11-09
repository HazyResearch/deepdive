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
for row in sys.stdin:
	# obj = json.loads(row)
	word_id, word, pos, ori_tag, idcol = row.rstrip().split('\t')
	tag = ori_tag
	# get tag
	# TODO json extractor bug...
	if ori_tag != '' and ori_tag not in ['NULL', '\N']:
		if (tag != 'NULL' and tag != '\N' and tag != 'O'): 
			tag = tag.split('-')[1]

		if tag not in tagNames: 
			tag = ''

		print '\t'.join([str(_) for _ in 
			sentID, word_id, word, pos, ori_tag, tagNames.index(tag),
			'\N'  # explicitly output NULL  for "id"
			])
	else:
		sentID += 1
		print '\t'.join([str(_) for _ in 
			'\N', word_id, '\N', '\N', '\N', tagNames.index(''), 
			'\N'  # explicitly output NULL  for "id"
			])
		# print json.dumps({
		# 	'sent_id' : None,
		# 	'word_id'	: obj['word_id'],
		# 	'word'		: None,
		# 	'pos'			: None,
		# 	'true_tag': None,
		# 	'tag'			: tagNames.index('')
		# })