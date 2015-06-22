#! /usr/bin/env python

import sys
import ddlib  # Load the ddlib Python library for NLP functions

# For each input row
for row in sys.stdin:
  # Parse tab-separated values
  column1, column2, column3, ... = row.strip().split('\t')

  # Output rows
  print '\t'.join(map(str, [
      column1,
      column2,
      column3
    ]))
