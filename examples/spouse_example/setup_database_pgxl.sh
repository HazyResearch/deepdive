#! /usr/bin/env bash

source env.sh

echo "Set DB_NAME to ${DBNAME}."
echo "HOST is ${PGHOST}, PORT is ${PGPORT}."

export APP_HOME=`cd $(dirname $0)/; pwd`

psql -h $PGHOST -p $PGPORT -U $PGUSER -d $DBNAME < $APP_HOME/schema_pgxl.sql
psql -h $PGHOST -p $PGPORT -U $PGUSER -d $DBNAME -c "copy articles from STDIN CSV;" < $APP_HOME/data/articles_dump.csv
psql -h $PGHOST -p $PGPORT -U $PGUSER -d $DBNAME -c "copy sentences from STDIN CSV;" < $APP_HOME/data/sentences_dump.csv
