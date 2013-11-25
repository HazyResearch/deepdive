#! /bin/bash

cd `dirname $0`
BASE_DIR=`pwd`

dropdb deepdive_titles
createdb deepdive_titles
psql -c "drop schema if exists public cascade; create schema public;" deepdive_titles
psql -c "create table titles(id bigserial primary key, title text, has_extractions boolean);" deepdive_titles
psql -c "create table words(id bigserial primary key, title_id bigint references titles(id), word text);" deepdive_titles
psql -c "COPY titles(title, has_extractions) FROM '$BASE_DIR/data/titles_taxonomy_extractions.csv' DELIMITER ',' CSV;" deepdive_titles