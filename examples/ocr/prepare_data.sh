#! /bin/bash

# Configuration
DB_NAME=deepdive_ocr
DB_USER=
DB_PASSWORD=

cd `dirname $0`
BASE_DIR=`pwd`

dropdb $DB_NAME
createdb $DB_NAME

psql -c "drop schema if exists public cascade; create schema public;" $DB_NAME

psql -c "create table features(id bigserial primary key, word_id int, feature_id int, feature_val boolean);" $DB_NAME
# psql -c "insert into people(id, name) values(6, 'Helen');" $DB_NAME

psql -c "create table feature_names(fid int primary key, fname varchar(20));" $DB_NAME
psql -c "create table label1(id bigserial primary key, wid int, val boolean);" $DB_NAME
psql -c "create table label2(id bigserial primary key, wid int, val boolean);" $DB_NAME

psql -c "COPY features(word_id, feature_id, feature_val) FROM '$BASE_DIR/data/raw/feature_table.csv' DELIMITER ',' CSV;" $DB_NAME
psql -c "COPY label1(wid, val) FROM '$BASE_DIR/data/raw/label1_table.csv' DELIMITER ',' CSV;" $DB_NAME
psql -c "COPY label2(wid, val) FROM '$BASE_DIR/data/raw/label2_table.csv' DELIMITER ',' CSV;" $DB_NAME

# COPY label1(wid, val) FROM '/Users/Robin/Documents/repos/deepdive_ocr/deepdive_danny/examples/ocr/data/raw/label1_table.csv' DELIMITER ',' CSV;