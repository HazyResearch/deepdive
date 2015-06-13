#!/usr/bin/env bash
# A script for running incremental phase
set -eux
cd "$(dirname "$0")"
. env.sh

DDlog=${1:-spouse_example.f2.ddl}
Base=${2:-inc-base.out}
Out=${3:-inc-delta.out}

export BASEDIR=$Base

./run.sh "$DDlog" --incremental initdb     "$Out"
./run.sh "$DDlog" --incremental extraction "$Out"
./run.sh "$DDlog" --incremental inference  "$Out"
