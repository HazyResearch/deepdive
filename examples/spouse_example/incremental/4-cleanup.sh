#!/usr/bin/env bash
set -eux
cd "$(dirname "$0")"
. env.sh

DDlog=${1:-$PWD/spouse_example.ddl}

./run.sh "$DDlog" --incremental cleanup
