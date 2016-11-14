#!/usr/bin/env bash
# A script to test whether the Greenplum tests should be done or not
set -eu

cd "$(dirname "$0")"
load() { source "$1".bash; }
load test_environ
{
    # gpfdist should be on PATH to say Greenplum is there
    type gpfdist
    # also check database version
    [[ "$(DBNAME=postgres timeout 1s db-execute "COPY (SELECT VERSION() LIKE '%Greenplum%') TO STDOUT")" == t ]]  # TODO move this check to db-init?
} &>/dev/null
