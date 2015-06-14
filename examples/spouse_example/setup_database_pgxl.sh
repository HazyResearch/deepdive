#! /usr/bin/env bash

source env.sh

echo "Set DB_NAME to ${DBNAME}."
echo "HOST is ${PGHOST}, PORT is ${PGPORT}."

export APP_HOME=`cd $(dirname $0)/; pwd`

psql -h $PGHOST -p $PGPORT -U $PGUSER -d $DBNAME < $APP_HOME/schema_pgxl.sql
bunzip2 <$APP_HOME/data/articles_dump.csv.bz2 |
psql -h $PGHOST -p $PGPORT -U $PGUSER -d $DBNAME -c "COPY articles FROM STDIN CSV"
bunzip2 <$APP_HOME/data/sentences_dump.csv.bz2 |
psql -h $PGHOST -p $PGPORT -U $PGUSER -d $DBNAME -c "COPY sentences FROM STDIN CSV"
