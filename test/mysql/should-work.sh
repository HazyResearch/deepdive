#!/usr/bin/env bash
# A script to test whether the MySQL tests should be done or not
set -eu

{
    cd "$(dirname "$0")"
    . ./env.sh

    # try executing a SQL query against the configured database for tests
    db-execute "SELECT VERSION()"
} &>/dev/null
