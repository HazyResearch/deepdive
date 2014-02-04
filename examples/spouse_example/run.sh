#! /bin/bash

export APP_HOME=`pwd`
export DEEPDIVE_HOME=`cd $(dirname $0)/../..; pwd`

# Database Configuration
export DBNAME=deepdive_spouse
export PGUSER=${PGUSER:-`whoami`}
export PGPASSWORD=${PGPASSWORD:-}

cd $DEEPDIVE_HOME

SBT_OPTS="-Xmx4g" sbt "run -c $APP_HOME/application.conf"