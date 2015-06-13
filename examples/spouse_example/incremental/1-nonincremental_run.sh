#!/usr/bin/env bash
# A script for running non-incremental run
set -eux
cd "$(dirname "$0")"
. env.sh

DDlog=${1:-$PWD/spouse_example.ddl}
Out=${2:-${DDlog%.ddl}.application.out}

./run.sh "$DDlog" --original extraction  -o "$Out"
./run.sh "$DDlog" --original inference   -o "$Out"
