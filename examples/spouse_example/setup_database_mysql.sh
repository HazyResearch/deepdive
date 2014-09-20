#! /usr/bin/env bash

set -e

if [ $# = 1 ]; then
  export DBNAME=$1
else
  echo "Usage: bash setup_database DBNAME"
  DBNAME=deepdive_spouse
fi
echo "Set DB_NAME to ${DBNAME}."
echo "HOST is ${DBHOST}, PORT is ${DBPORT}."

# TODO: do not use -p=${DBPASSWORD}
mysql -u ${DBUSER} -P ${DBPORT} -h ${DBHOST} -e "
  DROP DATABASE IF EXISTS $DBNAME;
  CREATE DATABASE $DBNAME;
  "

export APP_HOME=`cd $(dirname $0)/; pwd`

mysql -u ${DBUSER} -P ${DBPORT} -h ${DBHOST} $DBNAME < $APP_HOME/schema_mysql.sql

mysql -u ${DBUSER} -P ${DBPORT} -h ${DBHOST} $DBNAME -e "LOAD DATA INFILE '"$APP_HOME/data/articles_dump.csv"' 
INTO TABLE articles
  FIELDS TERMINATED BY ',' ENCLOSED BY '\"'
  LINES TERMINATED BY '\n'
  ;
"

# TODO currently all arrays are stored as strings
mysql -u ${DBUSER} -P ${DBPORT} -h ${DBHOST} $DBNAME -e "LOAD DATA INFILE '"$APP_HOME/data/sentences_dump_mysql.tsv"' 
INTO TABLE sentences(sentence_id, words, ner_tags)
  ;
"
