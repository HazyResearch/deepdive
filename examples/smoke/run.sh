#! /bin/bash

. "$(dirname $0)/env.sh"

export APP_HOME=`cd $(dirname $0); pwd`
export DEEPDIVE_HOME=`cd $(dirname $0)/../..; pwd`

bash $APP_HOME/prepare_data.sh
cd $DEEPDIVE_HOME

# env SBT_OPTS="-Xmx4g" $ROOT_PATH/sbt/sbt "run -c examples/smoke/application.conf"
deepdive -c $APP_HOME/application.conf
