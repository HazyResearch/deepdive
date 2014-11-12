#! /bin/bash

. env_mysql.sh

export APP_HOME=`cd $(dirname $0)/; pwd`
export DEEPDIVE_HOME=`cd $(dirname $0)/../../../; pwd`

# Database Configuration
export DBNAME=deepdive_spouse_tsv

# Initialize database
bash $APP_HOME/../setup_database_mysql.sh $DBNAME

# Using ddlib
export PYTHONPATH=$DEEPDIVE_HOME/ddlib:$PYTHONPATH

cd $DEEPDIVE_HOME

# deepdive -c $APP_HOME/application_mysql.conf

SBT_OPTS="-Xmx4g" sbt "run -c $APP_HOME/application_mysql.conf"

# DEBUG MODE
# SBT_OPTS="-agentlib:jdwp=transport=dt_socket,address=localhost:8000,server=y,suspend=y -Xmx4g" sbt "run -c $APP_HOME/application_mysql.conf"

