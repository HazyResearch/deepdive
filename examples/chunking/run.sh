#! /bin/bash

export DEEPDIVE_HOME=`cd $(dirname $0)/../..; pwd`
export APP_HOME=`pwd`

# Database Configuration
export DBNAME=chunking
export PGUSER=${PGUSER:-`whoami`}
export PGPASSWORD=${PGPASSWORD:-}
export PGPORT=${PGPORT:-5432}
export PGHOST=${PGHOST:-localhost}

$APP_HOME/prepare_data.sh

cd $DEEPDIVE_HOME
SBT_OPTS="-Xmx64g" sbt "run -c $APP_HOME/application.conf"
