#!/usr/bin/env bash
set -eu
cd "$(dirname "$0")"

deepdive sql update "drop table if exists result cascade;"
deepdive sql update "create table result(word_id bigint, word text, pos text, true_tag text, tag text);"

deepdive sql update "insert into result
  select b.word_id, b.word, b.pos, b.true_tag, b.category
  from (select word_id, max(expectation) as m
    from words_tag_inference group by word_id
  ) as a inner join words_tag_inference as b
  on a.word_id = b.word_id and a.m = b.expectation;"

deepdive sql update "copy (select word, pos, true_tag, max(tag) from result group by word_id, word, pos, true_tag order by word_id)
  to '$PWD/result' delimiter ' ';"

python convert.py

./conlleval.pl < output
