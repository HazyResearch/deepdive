#! /usr/bin/env bash

# Set username and password
export PGUSER=${PGUSER:-`whoami`}
export PGPASSWORD=${PGPASSWORD:-}
export PGPORT=${PGPORT:-5432}
export PGHOST=${PGHOST:-localhost}
export DBNAME=deepdive_test
export PGDATABASE=$DBNAME  # for testing to work with null settings
export GPHOST=${GPHOST:-localhost}
export GPPORT=${GPPORT:-8082}
export GPPATH=${GPPATH:-/tmp}

export DEEPDIVE_HOME=`cd $(dirname $0); pwd`

cd $DEEPDIVE_HOME/lib

case $(uname) in
  Darwin)
    export LD_LIBRARY_PATH=$DEEPDIVE_HOME/lib/dw_mac/lib/protobuf/lib:$DEEPDIVE_HOME/lib/dw_mac/lib:$LD_LIBRARY_PATH
    export DYLD_LIBRARY_PATH=$DEEPDIVE_HOME/lib/dw_mac:$DYLD_LIBRARY_PATH
    ;;

  Linux*)
    export LD_LIBRARY_PATH=$DEEPDIVE_HOME/lib/dw_linux/lib:$DEEPDIVE_HOME/lib/dw_linux/lib64:$LD_LIBRARY_PATH
    ;;

  *)
    echo >&2 "$(uname): Unsupported OS"
    false
esac

cd $DEEPDIVE_HOME

# Create test database
dropdb $DBNAME
createdb $DBNAME

# Run the test
# SBT_OPTS="-XX:MaxHeapSize=256m -Xmx512m -XX:MaxPermSize=256m" sbt "test"

# Separate different tests to fix the issue of unable to run multiple integration tests. If any of the tests return non-0 value, exit with the error code.
export SBT_OPTS="-XX:MaxHeapSize=256m -Xmx512m -XX:MaxPermSize=256m" 
sbt "test-only org.deepdive.test.unit.*" && sbt "test-only org.deepdive.test.integration.BiasedCoin" && sbt "test-only org.deepdive.test.integration.ChunkingApp"

