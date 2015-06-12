#! /bin/bash

. env.sh

export APP_HOME=`cd $(dirname $0)/; pwd`
export DEEPDIVE_HOME=`cd $(dirname $0)/../../../; pwd`
export BASEDIR=`cd $(dirname $0)/base; pwd`
export PYTHONPATH=$DEEPDIVE_HOME/ddlib:$PYTHONPATH

cd $DEEPDIVE_HOME

sh $APP_HOME/run.sh incremental_application.conf initdb inc
sh $APP_HOME/run.sh incremental_application.conf extraction inc
sh $APP_HOME/run.sh incremental_application.conf inference inc