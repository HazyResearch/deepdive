#!/usr/bin/env bash
# A script to generate a sentences dump that doesn't use Postgres ARRAYs for applications compiled from DDlog
set -eu
cd "$(dirname "$0")"

DBNAME=deepdive_spouse_data_$$
../setup_database.sh $DBNAME >&2
trap "dropdb $DBNAME" EXIT

psql $DBNAME -c "COPY (
SELECT
  document_id,
  sentence, 
  ARRAY_TO_STRING(words       , '~^~'),
  ARRAY_TO_STRING(lemma       , '~^~'),
  ARRAY_TO_STRING(pos_tags    , '~^~'),
  ARRAY_TO_STRING(dependencies, '~^~'),
  ARRAY_TO_STRING(ner_tags    , '~^~'),
  sentence_offset,
  sentence_id
FROM sentences
) TO STDOUT CSV"
