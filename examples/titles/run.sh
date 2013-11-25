#! /bin/bash

cd "$(dirname $0)/../..";
ROOT_PATH=`pwd`

$ROOT_PATH/examples/titles/prepare_data.sh
sbt "run -c examples/titles/application.conf"
