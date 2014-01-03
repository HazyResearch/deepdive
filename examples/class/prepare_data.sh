#! /bin/bash

# Configuration
DB_NAME=deepdive_class
DB_USER=
DB_PASSWORD=

cd `dirname $0`
BASE_DIR=`pwd`

dropdb deepdive_class
createdb deepdive_class

psql -c "drop schema if exists public cascade; create schema public;" $DB_NAME
