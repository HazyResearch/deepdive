#!/usr/bin/env bash
# A script for setting up the database for DDlog-based spouse example application
set -eux
cd "$(dirname "$0")"
. env.sh

DDlog=${1:-spouse_example.f1.ddlog}
Out=${2:-inc-base.out}

export BASEDIR=$Out

dropdb $DBNAME || true
createdb $DBNAME

./run.sh "$DDlog" "" initdb "$Out"
./setup_database.sh $DBNAME
