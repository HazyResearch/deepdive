#! /usr/bin/env bash

psql -c """DROP TABLE IF EXISTS has_spouse_features CASCADE;
CREATE TABLE has_spouse_features(
  id bigint,
  relation_id bigint references has_spouse(id),
  feature text);""" $DBNAME