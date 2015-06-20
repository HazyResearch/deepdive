#!/usr/bin/env bash
# A script for setting up the PostgreSQL database for DeepDive's spouse example
set -eux
cd "$(dirname "$0")"

if [ $# = 1 ]; then
  export DBNAME=$1
else
  echo "Usage: bash setup_database DBNAME"
  DBNAME=deepdive_spouse
fi
echo "Set DB_NAME to ${DBNAME}."
echo "HOST is ${PGHOST}, PORT is ${PGPORT}."

dropdb $DBNAME || true
createdb $DBNAME

psql -d $DBNAME <./schema.sql
bzcat ../../data/articles_dump.csv.bz2  | psql -d $DBNAME -c "COPY articles  FROM STDIN CSV"
bzcat ../../data/sentences_dump.csv.bz2 | psql -d $DBNAME -c "COPY sentences FROM STDIN CSV"
