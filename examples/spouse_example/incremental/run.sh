#! /bin/bash

export APP_HOME=`cd $(dirname $0)/; pwd`
export DEEPDIVE_HOME=`cd $(dirname $0)/../../../; pwd`

# Database Configuration
export DBNAME=deepdive_spouse_tsv

export PGUSER=${PGUSER:-`whoami`}
export PGPASSWORD=${PGPASSWORD:-}
export PGPORT=${PGPORT:-5432}
export PGHOST=${PGHOST:-localhost}
export ISINCREMENTAL=true
export LAST_FG=
# Initialize database
# bash $APP_HOME/../setup_database.sh $DBNAME

# Using ddlib
export PYTHONPATH=$DEEPDIVE_HOME/ddlib:$PYTHONPATH

cd $DEEPDIVE_HOME

# SBT_OPTS="-Xmx4g" sbt "run -c $APP_HOME/application.conf"
SBT_OPTS="-Xmx4g" sbt "run -c $APP_HOME/incremental_application.conf"
#deepdive -c $APP_HOME/application.conf
