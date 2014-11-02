#! /usr/bin/env bash

if [ $# = 1 ]; then
  export DBNAME=$1
else
  echo "Usage: bash setup_database DBNAME"
  DBNAME=deepdive_spouse
fi
echo "Set DB_NAME to ${DBNAME}."
echo "HOST is ${PGHOST}, PORT is ${PGPORT}."

dropdb $DBNAME
createdb $DBNAME

export APP_HOME=`cd $(dirname $0)/; pwd`

psql -d $DBNAME < $APP_HOME/schema.sql
psql -d $DBNAME -c "copy articles from STDIN CSV;" < $APP_HOME/data/articles_dump.csv
# sentence table format changed, dump is outdated in dependencies
psql -d $DBNAME -c "copy sentences(document_id, sentence, words, lemma,
  pos_tags, dep_paths, ner_tags, sentence_offset, sentence_id)
  FROM STDIN CSV;" < $APP_HOME/data/sentences_dump.csv
