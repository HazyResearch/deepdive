#! /bin/bash

cd "$(dirname $0)/../..";
ROOT_PATH=`pwd`

$ROOT_PATH/examples/titles/prepare_data.sh
env SBT_OPTS="-Xmx4g" sbt "run -c examples/titles/application.conf"
