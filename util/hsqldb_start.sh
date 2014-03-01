#! /usr/bin/env bash

export DEEPDIVE_HOME=`cd $(dirname $0)/../; pwd`

# Start the server
DBNAME=$1
echo "Starting HSQL with database=mem:$1"
java $JAVA_OPTS -cp $DEEPDIVE_HOME/lib/hsqldb.jar org.hsqldb.server.Server --database.0 mem:$1 --dbname.0 $1