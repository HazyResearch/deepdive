#!/usr/bin/python
# -*- coding: utf-8 -*-

import os,sys   # needed by most
import random   # random 
import yaml     # yaml parsing
import pprint   # pretty print

dirbase = 'boolean-f52-c3-m620/'
ids = [f.rstrip('.features.txt') for f in os.listdir(dirbase) if f.endswith('.features.txt')]
print 'Process files:', ids

wid = 1
fout = open('feature_table.csv', 'w')
for fid in ids:
  lines = open(dirbase + fid+'.features.txt').readlines()
  for l in lines: 
    vals = [b for b in l.strip().split('\t')]
    # print vals
    for sub in range(0, len(vals)):
      print >>fout, str(wid) + ',' + str(sub)+','+ str(vals[sub])
    wid += 1

totid = wid

wid = 1
fl1 = open('label1_table.csv', 'w')
fl2 = open('label2_table.csv', 'w')
for fid in ids:
  labels = [int(s) for s in open(dirbase + fid+'.labels.txt').readlines()]
  for l in labels: 
    l1 = False
    l2 = False
    if l == 1: l1 = True
    if l == 2: l2 = True
    print >>fl1, str(wid) + ',' + str(l1)
    print >>fl2, str(wid) + ',' + str(l2)
    wid += 1

fl1.close()
fl2.close()

