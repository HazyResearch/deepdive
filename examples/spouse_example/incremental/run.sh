#!/usr/bin/env bash
set -eux

DDlog=$1; shift
Mode=$1; shift
Pipeline=$1; shift

appConf="${DDlog%.ddl}.${Mode#--}_application.conf"
userConf="$(dirname "$DDlog")"/application.conf

# compile application.conf from DDlog if necessary
[[ "$appConf" -nt "$DDlog" && "$appConf" -nt "$userConf" ]] || {
    "$DEEPDIVE_HOME"/util/ddlog compile $Mode "$DDlog"
    cat "$userConf"
} >"$appConf"

# To run sbt, we must chdir to DEEPDIVE_HOME
appConf="$(cd "$(dirname "$appConf")" && pwd)/$(basename "$appConf")"
cd "$DEEPDIVE_HOME"

# ddlog-generated application.conf contains a PIPELINE, so we must set it here
export PIPELINE=$Pipeline

# run DeepDive, passing the rest of the arguments
sbt "run -c $appConf  $*"
