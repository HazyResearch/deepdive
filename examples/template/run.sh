#! /bin/bash

. "$(dirname $0)/env.sh"

cd $DEEPDIVE_HOME
### Run with deepdive binary:
deepdive -c $APP_HOME/application.conf
### Compile and run:
# sbt "run -c $APP_HOME/application.conf"