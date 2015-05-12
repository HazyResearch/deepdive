#! /bin/bash

export APP_HOME=`cd $(dirname $0)/; pwd`
export DEEPDIVE_HOME=`cd $(dirname $0)/../../../; pwd`
echo $DEEPDIVE_HOME

# Database Configuration
export DBNAME=deepdive_spouse

export PGUSER=${PGUSER:-`whoami`}
export PGPASSWORD=${PGPASSWORD:-}
export PGPORT=${PGPORT:-21050}
export PGHOST=${PGHOST:-localhost}

# Initialize database
#bash $APP_HOME/../setup_database_impala.sh $DBNAME

# Using ddlib
export PYTHONPATH=$DEEPDIVE_HOME/ddlib:$PYTHONPATH

cd $DEEPDIVE_HOME

# SBT_OPTS="-Xmx4g" sbt "run -c $APP_HOME/application.conf"
#bin/deepdive -c $APP_HOME/application_impala.conf
sbt/sbt "run -c $APP_HOME/application_impala.conf"
