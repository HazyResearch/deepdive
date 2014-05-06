#! /bin/bash

. "$(dirname $0)/env.sh"

cd "$(dirname $0)/../..";
ROOT_PATH=`pwd`

source $ROOT_PATH/examples/smoke/prepare_data.sh

env SBT_OPTS="-Xmx4g" sbt/sbt "run -c examples/smoke/application.conf"
