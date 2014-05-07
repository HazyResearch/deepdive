#! /usr/bin/env bash

psql -c """TRUNCATE has_spouse_features CASCADE;""" $DBNAME