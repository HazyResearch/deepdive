#! /usr/bin/env bash

# Set username and password
export PGUSER=${PGUSER:-`whoami`}
export PGPASSWORD=${PGPASSWORD:-}
export PGPORT=${PGPORT:-5432}
export PGHOST=${PGHOST:-localhost}
export DBNAME=deepdive_test
export PGDATABASE=$DBNAME  # for testing to work with null settings

export DEEPDIVE_HOME=`cd $(dirname $0); pwd`

cd $DEEPDIVE_HOME/lib

# Add dependencies for Travis system

# Detect OS
if [ "$(uname)" == "Darwin" ]; then
  # if haven't unzipped dw_mac.zip
  if [ ! -d $DEEPDIVE_HOME/lib/dw_mac ]; then
      unzip dw_mac.zip
    fi

    export LD_LIBRARY_PATH=$DEEPDIVE_HOME/lib/dw_mac/lib/protobuf/lib:$DEEPDIVE_HOME/lib/dw_mac/lib:$LD_LIBRARY_PATH
  export DYLD_LIBRARY_PATH=$DEEPDIVE_HOME/lib/dw_mac:$DYLD_LIBRARY_PATH

elif [ "$(expr substr $(uname -s) 1 5)" == "Linux" ]; then
    # if haven't unzipped dw_linux.zip
  if [ ! -d $DEEPDIVE_HOME/lib/dw_linux ]; then
      unzip dw_linux.zip
    fi

    export LD_LIBRARY_PATH=$DEEPDIVE_HOME/lib/dw_linux/lib:$DEEPDIVE_HOME/lib/dw_linux/lib64:$LD_LIBRARY_PATH
fi

echo $LD_LIBRARY_PATH



cd $DEEPDIVE_HOME

# Create test database
dropdb $DBNAME
createdb $DBNAME

# Run the test
SBT_OPTS="-Xmx2g" sbt "test"
