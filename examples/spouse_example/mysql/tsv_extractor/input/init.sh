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
bzcat ./sentences_dump_mysql.tsv.bz2 |
if [[ -z ${SUBSAMPLE_NUM_SENTENCES:-} ]]; then cat; else head -n ${SUBSAMPLE_NUM_SENTENCES}; fi |
deepdive sql "
    LOAD DATA LOCAL INFILE '/dev/stdin'
    INTO TABLE sentences(sentence_id, words, ner_tags)
    "

./prepare_data.sh
