#! /usr/bin/env bash

export SBT_OPTS="-Xmx1g"

$(dirname $0)/target/start $@