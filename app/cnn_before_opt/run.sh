#! /bin/bash

# export CATALINA_OPTS="-Xmx256g";
# export JAVA_OPTS="-Xmx256g"

. "$(dirname $0)/env.sh"

cd $DEEPDIVE_HOME
sbt "run -c $APP_HOME/application.conf"