#! /usr/bin/env python

import sys

tagNames = ['NP', 'VP', 'PP', 'ADJP', 'ADVP', 'SBAR', 'O', 'PRT', 'CONJP', 'INTJ', 'LST', 'B', '']

fin = open(sys.argv[1], 'r')

lastTag = ''

for line in fin:
  tokens = line.rstrip().split('\t')
  
  # empty line
  if (tokens[0] in ['\N', 'NULL']): # for compatibility with MySQL
    print
    lastTag = ''
    continue

  # normal word
  tag = tagNames[int(tokens[3])]
  if tag != 'O': 
    if lastTag == tag:
      tag = 'I-' + tag
    else:
      tag = 'B-' + tag
  lastTag = tagNames[int(tokens[3])]
  tokens[3] = tag
  print ' '.join(tokens)