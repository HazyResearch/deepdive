#!/usr/bin/env bats
# Tests for deepdive db init

. "$BATS_TEST_DIRNAME"/env.sh >&2

setup() {
    cd "$BATS_TEST_DIRNAME"/spouse_example
}

@test "$DBVARIANT schema_json_to_sql a single table" {
    cd ddlog || skip
    tmp=$(mktemp -d "${TMPDIR:-/tmp}"/deepdive-initdb.XXXXXXX)
    schema_json="$tmp"/schema.json
    ddlog export-schema app.ddlog > "$schema_json"
    expected='DROP TABLE IF EXISTS articles CASCADE; CREATE TABLE articles(article_id text, text text);'
    [[ $(schema_json_to_sql $schema_json articles) = "$expected" ]]
}

@test "$DBVARIANT schema_json_to_sql without arguments" {
    cd ddlog || skip
    tmp=$(mktemp -d "${TMPDIR:-/tmp}"/deepdive-initdb.XXXXXXX)
    schema_json="$tmp"/schema.json
    ddlog export-schema app.ddlog > "$schema_json"
    expected='DROP TABLE IF EXISTS articles CASCADE;'
    expected+=' CREATE TABLE articles(article_id text, text text);'
    expected+=' DROP TABLE IF EXISTS people_mentions CASCADE;'
    expected+=' CREATE TABLE people_mentions(sentence_id text, start_position int, length int, text text, mention_id text);'
    expected+=' DROP TABLE IF EXISTS has_spouse_features CASCADE;'
    expected+=' CREATE TABLE has_spouse_features(relation_id text, feature text);'
    expected+=' DROP TABLE IF EXISTS has_spouse CASCADE;'
    expected+=' CREATE TABLE has_spouse(relation_id text, id bigint, label boolean);'
    expected+=' DROP TABLE IF EXISTS has_spouse_candidates CASCADE;'
    expected+=' CREATE TABLE has_spouse_candidates(person1_id text, person2_id text, sentence_id text, description text, relation_id text, is_true boolean);'
    expected+=' DROP TABLE IF EXISTS sentences CASCADE;'
    expected+=' CREATE TABLE sentences(document_id text, sentence text, words text[], lemma text[], pos_tags text[], dependencies text[], ner_tags text[], sentence_offset int, sentence_id text);'
    [[ $(schema_json_to_sql $schema_json) = "$expected" ]]
}

@test "$DBVARIANT initdb from ddlog" {
    cd ddlog || skip
    deepdive compile
    deepdive db init
    deepdive create table articles
    [[ $(deepdive sql eval "SELECT * FROM articles" format=csv header=1) = 'article_id,text' ]]
    deepdive sql "INSERT INTO articles VALUES ('foo', 'bar')"
}
