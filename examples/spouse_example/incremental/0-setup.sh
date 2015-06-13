#!/usr/bin/env bash
# A script for setting up the database for DDlog-based incremental application
set -eux
cd "$(dirname "$0")"
. env.sh

DDlog=${1:-$PWD/spouse_example.ddl}
Out=${2:-${DDlog%.ddl}.base.application.out}
Mode=${3:---materialization}

dropdb --if-exists $DBNAME
createdb $DBNAME

export BASEDIR=$Out

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
