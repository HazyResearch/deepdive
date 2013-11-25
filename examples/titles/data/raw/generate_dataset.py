#! /usr/bin/env python

import csv
import sys
import codecs

titles = {}
with open("titles.csv", "rU") as csvfile:
  reader = csv.reader(csvfile, quotechar="\"", escapechar="\\")
  for row in [x for x in reader if len(x) > 0]:
    titles[row[0]] = False

with open("extractions.csv", "r") as csvfile:
  reader = csv.reader(csvfile, quotechar="\"", escapechar="\\")
  csvfile.next()
  for row in reader:
    ref_no, title, journal_title, taxonomy_relations, temporal_relations, geology_formations, geology_locations = row
    if int(taxonomy_relations) > 0:
      titles[title] = True
    else:
      titles[title] = False

for k,v in titles.items():
  writer = csv.writer(sys.stdout, quotechar="\"", escapechar="\\")
  writer.writerow([k, v])