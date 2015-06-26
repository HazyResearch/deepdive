#!/usr/bin/env bash
# A script for loading DeepDive spouse example data into PostgreSQL database
set -eux
cd "$(dirname "$0")"

bzcat ./articles_dump.csv.bz2  | deepdive sql "COPY articles  FROM STDIN CSV"
bzcat ./sentences_dump.csv.bz2 |
if [[ -z ${SUBSAMPLE_NUM_SENTENCES:-} ]]; then cat; else head -n ${SUBSAMPLE_NUM_SENTENCES}; fi |
deepdive sql "COPY sentences FROM STDIN CSV"

./prepare_data.sh
