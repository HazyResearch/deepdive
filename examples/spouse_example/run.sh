#! /bin/bash

export APP_HOME=`pwd`
export DEEPDIVE_HOME=`cd $(dirname $0)/../..; pwd`

export LD_LIBRARY_PATH="/dfs/rulk/0/hazy_share/lib64/:/dfs/rulk/0/hazy_share/lib/protobuf/lib/:/dfs/rulk/0/hazy_share/lib/tclap/lib/:$LD_LIBRARY_PATH"

# Database Configuration
export DBNAME=deepdive_spouse
export PGUSER=${PGUSER:-`whoami`}
export PGPASSWORD=${PGPASSWORD:-}
export PGPORT=${PGPORT:-5432}
export PGHOST=${PGHOST:-localhost}

cd $DEEPDIVE_HOME

SBT_OPTS="-Xmx4g" sbt "run -c $APP_HOME/application.conf"
