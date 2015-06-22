#!/usr/bin/env bash
# A script for initializing database and loading data into it
set -eu
cd "$(dirname "$0")"

# TODO use `deepdive sql update "COPY ... FROM STDIN"` command to load initial data from files
# deepdive sql update "COPY table_name FROM STDIN" <table_name.tsv
