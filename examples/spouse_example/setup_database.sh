#! /usr/bin/env bash

export DBNAME=deepdive_spouse
export PGUSER=${PGUSER:-`whoami`}
export PGPASSWORD=${PGPASSWORD:-}

dropdb deepdive_spouse
createdb deepdive_spouse

psql -d deepdive_spouse < schema.sql
psql -d deepdive_spouse -c "copy articles from STDIN CSV;" < data/articles_dump.csv
psql -d deepdive_spouse -c "copy sentences from STDIN CSV;" < data/sentences_dump.csv