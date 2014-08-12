#! /usr/bin/env bash

export DBNAME=chunking

psql -c "drop table if exists result cascade;" $DBNAME
psql -c "create table result(word_id bigint, word text, pos text, true_tag text, tag text);" $DBNAME

psql -c """insert into result 
  select b.word_id, b.word, b.pos, b.true_tag, b.category
  from (select word_id, max(expectation) as m 
    from words_tag_inference group by word_id 
  ) as a inner join words_tag_inference as b
  on a.word_id = b.word_id and a.m = b.expectation;""" $DBNAME

psql -c """copy (select word, pos, true_tag, max(tag) from result group by word_id, word, pos, true_tag order by word_id)
  to '$PWD/result' delimiter ' ';""" $DBNAME

python convert.py

./conlleval.pl < output
