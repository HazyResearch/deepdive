#!/usr/bin/env bash
set -eux

DDlog=$1; shift
Mode=$1; shift
Pipeline=$1; shift

appConf="$PWD/${DDlog%.ddl}.${Mode#--}_application.conf"

# compile application.conf from DDlog if necessary
[[ "$appConf" -nt "$DDlog" ]] ||
    java -jar $DEEPDIVE_HOME/util/ddlog.jar compile $Mode "$DDlog" >"$appConf"

cd "$DEEPDIVE_HOME"
export PIPELINE=$Pipeline

# run DeepDive, passing the rest of the arguments
sbt "run -c $appConf  $*"
