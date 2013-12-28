#! /bin/bash

# Configuration
DB_NAME=deepdive_titles
DB_USER=
DB_PASSWORD=

cd `dirname $0`
BASE_DIR=`pwd`

dropdb deepdive_titles
createdb deepdive_titles
psql -c "drop schema if exists public cascade; create schema public;" $DB_NAME
psql -c "create table titles_initial(id bigserial primary key, title text, has_extractions boolean);" $DB_NAME
psql -c "create table titles(id bigserial primary key, title text, has_extractions boolean);" $DB_NAME
psql -c "create table words(id bigserial primary key, title_id bigint references titles(id), word text, is_present boolean);" $DB_NAME
psql -c "COPY titles_initial(title, has_extractions) FROM '$BASE_DIR/data/titles_taxonomy_extractions_sample.csv' DELIMITER ',' CSV;" $DB_NAME