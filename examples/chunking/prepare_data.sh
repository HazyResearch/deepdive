#! /usr/bin/env bash

export DBNAME=chunking

APP_HOME=`pwd`

dropdb $DBNAME
createdb $DBNAME

psql -c """create table words_raw(
	word_id bigserial,
	word text,
	pos text,
	tag text,
	id bigint);""" $DBNAME

psql -c """create table words(
	sent_id bigint,
	word_id bigint,
	word text,
	pos text,
	true_tag text,
	tag int,
	id bigint);""" $DBNAME

psql -c """create table word_features(
	word_id bigint,
	feature text,
	id bigint);""" $DBNAME

psql -c "copy words_raw(word, pos, tag) from '$APP_HOME/data/train_null_terminated.txt' delimiter ' ' null 'null';" $DBNAME
psql -c "copy words_raw(word, pos, tag) from '$APP_HOME/data/test_null_terminated.txt' delimiter ' ' null 'null';" $DBNAME