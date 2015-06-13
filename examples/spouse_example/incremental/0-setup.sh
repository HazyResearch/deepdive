#!/usr/bin/env bash
# A script for setting up the database for DDlog-based spouse example application
set -eux
cd "$(dirname "$0")"
. env.sh

DDlog=${1:-spouse_example.f1.ddl}
Out=${2:-inc-base.out}

export BASEDIR=$Out

dropdb --if-exists $DBNAME
createdb $DBNAME

./run.sh "$DDlog" "" initdb "$Out"
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
