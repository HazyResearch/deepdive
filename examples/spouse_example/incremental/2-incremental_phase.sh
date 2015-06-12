#!/usr/bin/env bash
# A script for running incremental phase
set -eux
cd "$(dirname "$0")"
. env.sh

DDlog=${1:-$PWD/spouse_example.f2.ddl}
Out=${2:-${DDlog%.ddl}.inc.out}

./run.sh "$DDlog" --incremental initdb      -o "$Out"
./run.sh "$DDlog" --incremental extraction  -o "$Out"
./run.sh "$DDlog" --incremental inference   -o "$Out"
