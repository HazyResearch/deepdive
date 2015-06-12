#! /bin/bash

. env.sh

export APP_HOME=`cd $(dirname $0)/; pwd`
export DEEPDIVE_HOME=`cd $(dirname $0)/../../../; pwd`
export BASEDIR=`cd $(dirname $0)/base; pwd`
export PYTHONPATH=$DEEPDIVE_HOME/ddlib:$PYTHONPATH

export DEEPDIVE_ACTIVE_INCREMENTAL_VARIABLES="has_spouse"
export DEEPDIVE_ACTIVE_INCREMENTAL_RULES="has_spouse_0"

cd $DEEPDIVE_HOME

# dropdb $DBNAME
# createdb $DBNAME
# sh $APP_HOME/run.sh materialization_application.conf initdb
# psql -d $DBNAME -c "copy sentences from STDIN DELIMITER ',';" < $APP_HOME/../data/sentences_dump_inc.csv
# sh $APP_HOME/run.sh materialization_application.conf extraction
sh $APP_HOME/run.sh materialization_application.conf inference