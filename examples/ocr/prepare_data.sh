#! /bin/bash

# Configuration
DB_NAME=deepdive_ocr

cd `dirname $0`
BASE_DIR=`pwd`

dropdb $DB_NAME
createdb $DB_NAME

psql -c """
    DROP SCHEMA IF EXISTS public CASCADE; 
    CREATE SCHEMA public;
    """ $DB_NAME

psql -c """
    CREATE TABLE features(
    id          BIGSERIAL PRIMARY KEY, 
    word_id     INT, 
    feature_id  INT, 
    feature_val BOOLEAN);
    """ $DB_NAME

psql -c """
    CREATE TABLE feature_names(
    fid     INT PRIMARY KEY, 
    fname   VARCHAR(20));
    """ $DB_NAME
psql -c """
    CREATE TABLE label1(
    wid   INT, val BOOLEAN,
    id    BIGINT);
    """ $DB_NAME
psql -c """
    CREATE TABLE label2(
    wid   INT, val BOOLEAN,
    id    BIGINT);
    """ $DB_NAME

psql -c """
    COPY features(word_id, feature_id, feature_val) 
    FROM STDIN DELIMITER ',' CSV;
    """ $DB_NAME <$BASE_DIR/data/raw/feature_table.csv
psql -c """
    COPY label1(wid, val) 
    FROM STDIN DELIMITER ',' CSV;
    """ $DB_NAME <$BASE_DIR/data/raw/label1_table.csv
psql -c """
    COPY label2(wid, val) 
    FROM STDIN DELIMITER ',' CSV;
    """ $DB_NAME <$BASE_DIR/data/raw/label2_table.csv
