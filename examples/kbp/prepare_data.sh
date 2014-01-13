#! /bin/bash

# generate the csv file from the raw data
python generate_csv_data.py

# Configuration
DB_NAME=deepdive_kbp
DB_USER=
DB_PASSWORD=

cd `dirname $0`
BASE_DIR=`pwd`

dropdb deepdive_kbp
createdb deepdive_kbp

psql -c "drop schema if exists public cascade; create schema public;" $DB_NAME

psql -c "create type valid_feature_type as enum ('PERSON', 'LOCATION', 'ORGANIZATION');" $DB_NAME

psql -c "create table mention(id bigserial primary key, sentence_id bigserial, doc_id text, text_contents text, feature_type valid_feature_type);" $DB_NAME

#psql -c "COPY mention(sentence_id, doc_id, text_contents, feature_type) FROM '$BASE_DIR/data/kbp_person_entities.csv' DELIMITER '~' CSV;" $DB_NAME