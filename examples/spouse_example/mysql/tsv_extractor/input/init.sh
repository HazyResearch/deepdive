#!/usr/bin/env bash
# A script for loading DeepDive spouse example data into MySQL database
set -eux
cd "$(dirname "$0")"

bzcat ./articles_dump.csv.bz2 |
deepdive sql "
    LOAD DATA LOCAL INFILE '/dev/stdin'
    INTO TABLE articles
      FIELDS TERMINATED BY ',' ENCLOSED BY '\"'
      LINES TERMINATED BY '\n'
    "
bzcat ./sentences_dump.csv.bz2 |
deepdive sql "
    LOAD DATA LOCAL INFILE '/dev/stdin'
    INTO TABLE sentences
      FIELDS TERMINATED BY ',' ENCLOSED BY '\"'
      LINES TERMINATED BY '\n'
    "

./prepare_data.sh
