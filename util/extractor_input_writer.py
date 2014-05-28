#! /usr/bin/env python

# This script helps to debug extractors (json_extractor or tsv_extractor) by simply printing input lines to a file, specified by a command line argument.

import sys

if len(sys.argv) != 2:
  print >>sys.stderr, "Usage:", sys.argv[0], "SAMPLE_FILE_PATH"
  sys.exit(1)

fout = open(sys.argv[1], 'w')
print >>sys.stderr, "Writing extractor input to file:",sys.argv[1]
for line in sys.stdin:
  print >>fout, line.rstrip('\n')

fout.close()
