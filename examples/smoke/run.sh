#! /bin/bash

cd "$(dirname $0)/../..";
ROOT_PATH=`pwd`

export DBNAME=deepdive_smoke
export PGUSER=${PGUSER:-`whoami`}
export PGPASSWORD=${PGPASSWORD:-}
export PGPORT=${PGPORT:-5432}
export PGHOST=${PGHOST:-localhost}

# The following steps are necessary to set up the environment variables for the DimmWitted sampler
cd $ROOT_PATH/lib

# Detect OS
if [ "$(uname)" == "Darwin" ]; then
    unzip dw_mac.zip
    export LD_LIBRARY_PATH=[DEEPDIVE_HOME]/lib/dw_mac/lib/protobuf/lib:[DEEPDIVE_HOME]/lib/dw_mac/lib
	export DYLD_LIBRARY_PATH=[DEEPDIVE_HOME]/lib/dw_mac
elif [ "$(expr substr $(uname -s) 1 5)" == "Linux" ]; then
    unzip dw_linux.zip
    export LD_LIBRARY_PATH=[DEEPDIVE_HOME]/lib/dw_linux/lib:[DEEPDIVE_HOME]/lib/dw_linux/lib64

cd $ROOT_PATH

source $ROOT_PATH/examples/smoke/prepare_data.sh

env SBT_OPTS="-Xmx4g" sbt/sbt "run -c examples/smoke/application.conf"
