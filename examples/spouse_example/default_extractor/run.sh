#! /bin/bash

. "$(dirname $0)/env.sh"
echo $DBNAME

export APP_HOME=`cd $(dirname $0)/; pwd`
export DEEPDIVE_HOME=`cd $(dirname $0)/../../../; pwd`

# Initialize database
bash $APP_HOME/../setup_database.sh $DBNAME

# Using ddlib
export PYTHONPATH=$DEEPDIVE_HOME/ddlib:$PYTHONPATH

cd $DEEPDIVE_HOME

### Run with deepdive binary:
deepdive -c $APP_HOME/application.conf
### Compile and run:
# sbt "run -c $APP_HOME/application.conf"