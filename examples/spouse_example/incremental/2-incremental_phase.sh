#!/usr/bin/env bash
# A script for running incremental phase
set -eux
cd "$(dirname "$0")"
. env.sh

DDlog=${1:-$PWD/spouse_example.f2.ddl}
Out=${2:-${DDlog%.ddl}.inc.application.out}
Base=${3:-${DDlog%.*.ddl}.base.application.out}

export BASEDIR=$Base

./run.sh "$DDlog" --incremental initdb      -o "$Out"
./run.sh "$DDlog" --incremental extraction  -o "$Out"
./run.sh "$DDlog" --incremental inference   -o "$Out"
