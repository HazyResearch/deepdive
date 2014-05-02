#! /usr/bin/env bash

psql -c """DROP TABLE IF EXISTS has_spouse CASCADE;
CREATE TABLE has_spouse(
  id bigint, 
  person1_id bigint references people_mentions(id),
  person2_id bigint references people_mentions(id),
  sentence_id bigint references sentences(id),
  description text,
  is_true boolean);
""" deepdive_spouse
