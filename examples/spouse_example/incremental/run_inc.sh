#!/usr/bin/env bash
# A script for running incremental phase
set -eux
cd "$(dirname "$0")"
. env.sh

./run.sh spouse_example.ddl --incremental initdb
./run.sh spouse_example.ddl --incremental extraction
./run.sh spouse_example.ddl --incremental inference
