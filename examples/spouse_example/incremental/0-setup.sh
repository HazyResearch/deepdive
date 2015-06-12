#!/usr/bin/env bash
# A script for setting up the database for incremental application
set -eux
cd "$(dirname "$0")"
. env.sh

dropdb --if-exists $DBNAME
createdb $DBNAME

./run.sh spouse_example.ddl --materialization initdb
psql -d $DBNAME -c "COPY SENTENCES FROM STDIN DELIMITER ',';" < ../data/sentences_dump_inc.csv
