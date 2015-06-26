#!/usr/bin/env bash
# A script to test whether the MySQL tests should be done or not
set -eu

{
    cd "$(dirname "$0")"
    . ./env.sh

    # try initializing the configured database for tests
    db-init
} &>/dev/null
