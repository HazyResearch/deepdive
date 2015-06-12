#! /bin/bash

. env.sh

export APP_HOME=`cd $(dirname $0)/; pwd`
export DEEPDIVE_HOME=`cd $(dirname $0)/../../../; pwd`
export BASEDIR=`cd $(dirname $0)/base; pwd`
export PYTHONPATH=$DEEPDIVE_HOME/ddlib:$PYTHONPATH

cd $DEEPDIVE_HOME

dropdb $DBNAME
createdb $DBNAME
sh $APP_HOME/run.sh materialization_application.conf initdb
psql -d $DBNAME -c "copy sentences from STDIN DELIMITER ',';" < $APP_HOME/../data/sentences_dump_inc.csv