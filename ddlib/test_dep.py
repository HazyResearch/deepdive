#! /usr/bin/env python
# File: udf/ext_has_spouse_features.py

import sys, json
import ddlib


def my_dep_format_parser(s):
	parent, label, child = s.split('\t')
	return (int(parent)-1, label, int(child)-1)

for row in sys.stdin:

	obj = json.loads(row)

	words = ddlib.unpack_words(obj, character_offset_begin='character_offset_begin', 
		character_offset_end='character_offset_end', lemma='lemma', 
		pos='pos', words = 'words', dep_graph = 'dep_graph', dep_graph_parser=my_dep_format_parser)

	edges = ddlib.dep_path_between_words(words, 0, len(words)-1)

	for e in edges:
		print e.word1.lemma, e.word2.lemma

