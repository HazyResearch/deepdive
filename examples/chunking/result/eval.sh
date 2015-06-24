#!/usr/bin/env bash
set -eu
cd "$(dirname "$0")"

deepdive sql execute "drop table if exists result cascade;"
deepdive sql execute "create table result(word_id bigint, word text, pos text, true_tag text, tag text);"

deepdive sql execute "insert into result
  select b.word_id, b.word, b.pos, b.true_tag, b.category
  from (select word_id, max(expectation) as m
    from words_tag_inference group by word_id
  ) as a inner join words_tag_inference as b
  on a.word_id = b.word_id and a.m = b.expectation;"

deepdive sql execute "copy (select word, pos, true_tag, max(tag) from result group by word_id, word, pos, true_tag order by word_id)
  to '$PWD/result' delimiter ' ';"

python convert.py

./conlleval.pl < output
