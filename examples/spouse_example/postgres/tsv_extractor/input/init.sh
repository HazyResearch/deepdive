#!/usr/bin/env bash
# A script for loading DeepDive spouse example data into PostgreSQL database
set -eux
cd "$(dirname "$0")"

bzcat ./articles_dump.csv.bz2  | deepdive sql update "COPY articles  FROM STDIN CSV"
bzcat ./sentences_dump.csv.bz2 | deepdive sql update "COPY sentences FROM STDIN CSV"

./prepare_data.sh
