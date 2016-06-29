#! /usr/bin/env python

fin = open('result', 'r')
fout = open('output', 'w')

lastTag = ''

for line in fin:
  tokens = line.split(' ')

  # empty line
  if (tokens[0] in ['\N', 'NULL']): # for compatibility with MySQL
  # if (tokens[0] == '\N'):
    fout.write('\n')
    lastTag = ''
    continue

  # normal word
  tag = tokens[3]
  if tag != 'O':
    if lastTag == tag:
      tag = 'I-' + tag
    else:
      tag = 'B-' + tag
  lastTag = tokens[3]
  tokens[3] = tag
  fout.write(' '.join(tokens) + '\n')
