#! /bin/bash

cd "$(dirname $0)/../..";
ROOT_PATH=`pwd`

export DBNAME=deepdive_smoke
export PGUSER=${PGUSER:-`whoami`}
export PGPASSWORD=${PGPASSWORD:-}
export PGPORT=${PGPORT:-5432}
export PGHOST=${PGHOST:-localhost}

$ROOT_PATH/examples/smoke/prepare_data.sh
env SBT_OPTS="-Xmx4g" sbt "run -c examples/smoke/application.conf"
