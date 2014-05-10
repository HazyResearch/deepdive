#! /usr/bin/env bash

. "env.sh"

dropdb --if-exists $PGDATABASE
createdb $PGDATABSE

psql < schema.sql