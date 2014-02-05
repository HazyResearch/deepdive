#! /bin/bash

export DEEPDIVE_HOME=`cd $(dirname $0)/../..; pwd`
export APP_HOME=`pwd`

# Database Configuration
export DBNAME=
export PGUSER=${PGUSER:-`whoami`}
export PGPASSWORD=${PGPASSWORD:-}

cd $DEEPDIVE_HOME

SBT_OPTS="-Xmx4g" sbt "run -c $APP_HOME/application.conf"