#!/usr/bin/env bash
set -eu
cd "$(dirname "$0")"
if [[ -n ${SUBSAMPLE_NUM_ARTICLES:-} ]]; then
    set -x
    # parse CSV in Python to take the first few rows
    bzcat ./articles_dump.csv.bz2 | python -c '
import sys,csv
limit = int(sys.argv[1])
i = 0
out = csv.writer(sys.stdout)
for row in csv.reader(sys.stdin):
  if i > limit:
    break
  out.writerow(row)
  i = i + 1
' ${SUBSAMPLE_NUM_ARTICLES}
else
    bzcat ./articles_dump.csv.bz2
fi
