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
	# if haven't unzipped dw_mac.zip
	if [ ! -d $ROOT_PATH/lib/dw_mac ]; then
    	unzip dw_mac.zip
    fi

    export LD_LIBRARY_PATH=$ROOT_PATH/lib/dw_mac/lib/protobuf/lib:$ROOT_PATH/lib/dw_mac/lib
	export DYLD_LIBRARY_PATH=$ROOT_PATH/lib/dw_mac

elif [ "$(expr substr $(uname -s) 1 5)" == "Linux" ]; then
    # if haven't unzipped dw_linux.zip
	if [ ! -d $ROOT_PATH/lib/dw_linux ]; then
    	unzip dw_linux.zip
    fi

    export LD_LIBRARY_PATH=$ROOT_PATH/lib/dw_linux/lib:$ROOT_PATH/lib/dw_linux/lib64
fi

cd $ROOT_PATH

source $ROOT_PATH/examples/smoke/prepare_data.sh

env SBT_OPTS="-Xmx4g" sbt/sbt "run -c examples/smoke/application.conf"
