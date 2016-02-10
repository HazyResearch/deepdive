#!/usr/bin/env bash
# A script for initializing an application
set -eu
cd "$(dirname "$0")"

# TODO use `deepdive load TABLE FILE` command to load initial data from files
# deepdive load "foo" foo.tsv
# deepdive load "foo(column1,col2,col3)" foo_three_columns.tsv
