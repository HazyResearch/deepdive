#!/usr/bin/env bash
# A script for running DDlog spouse example non-incrementally
set -eux
cd "$(dirname "$0")"
. env.sh

DDlog=${1:-spouse_example.f1+f2.ddl}
Out=${2:-${DDlog%.ddl}.out}

./run.sh "$DDlog" --original extraction "$Out"
./run.sh "$DDlog" --original inference  "$Out"
