#!/usr/bin/env bash
# A script to test whether the PostgreSQL tests should be done or not
set -eu

{
    cd "$(dirname "$0")"
    . ./env.sh

    # try executing a SQL query against the configured database for tests
    DBNAME=postgres db-execute "SELECT VERSION() LIKE '%Postgres%'"
} &>/dev/null
