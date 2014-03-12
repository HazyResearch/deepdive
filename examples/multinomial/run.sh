#! /bin/bash

. "$(dirname $0)/env.sh"

cd $DEEPDIVE_HOME

dropdb $DBNAME
createdb $DBNAME
psql -d $DBNAME < $APP_HOME/schema.sql
psql -d $DBNAME < $APP_HOME/data.sql

sbt "run -c $APP_HOME/application.conf"