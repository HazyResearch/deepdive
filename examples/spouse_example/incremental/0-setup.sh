#!/usr/bin/env bash
# A script for setting up the database for DDlog-based incremental application
set -eux
cd "$(dirname "$0")"
. env.sh

DDlog=${1:-$PWD/spouse_example.ddl}
Mode=${2:---materialization}

dropdb --if-exists $DBNAME
createdb $DBNAME

./run.sh "$DDlog" $Mode initdb
./generate-sentences_dump_noarray.csv.sh |
psql $DBNAME -c "COPY sentences(
    document_id,
    sentence,
    words,
    lemma,
    pos_tags,
    dependencies,
    ner_tags,
    sentence_offset,
    sentence_id
) FROM STDIN CSV"

psql $DBNAME -c "UPDATE sentences SET dd_count = 1" &>/dev/null || true
