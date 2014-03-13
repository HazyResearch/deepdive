#! /bin/bash

# Configuration
DB_NAME=deepdive_smoke

cd `dirname $0`
BASE_DIR=`pwd`

dropdb deepdive_smoke
createdb deepdive_smoke

psql -c "drop schema if exists public cascade; create schema public;" $DB_NAME

psql -c "create table people1(id bigserial primary key, person_id bigint, name text, has_cancer boolean);" $DB_NAME
psql -c "create table people2(id bigserial primary key, person_id bigint, name text, smokes boolean);" $DB_NAME
psql -c "create table friends(id bigserial primary key, person_id bigint, friend_id bigserial);" $DB_NAME

psql -c "insert into people1(id, person_id, name) values (1, 1, 'Anna');" $DB_NAME
psql -c "insert into people1(id, person_id, name) values (2, 2, 'Bob');" $DB_NAME
psql -c "insert into people1(id, person_id, name) values(3, 3, 'Edward');" $DB_NAME
psql -c "insert into people1(id, person_id, name) values(4, 4, 'Frank');" $DB_NAME
psql -c "insert into people1(id, person_id, name) values(5, 5, 'Gary');" $DB_NAME
psql -c "insert into people1(id, person_id, name) values(6, 6, 'Helen');" $DB_NAME

psql -c "insert into people2(id, person_id, name, smokes) values (7, 1, 'Anna', TRUE);" $DB_NAME
psql -c "insert into people2(id, person_id, name) values (8, 2, 'Bob');" $DB_NAME
psql -c "insert into people2(id, person_id, name, smokes) values(9, 3, 'Edward', TRUE);" $DB_NAME
psql -c "insert into people2(id, person_id, name) values(10, 4, 'Frank');" $DB_NAME
psql -c "insert into people2(id, person_id, name) values(11, 5, 'Gary');" $DB_NAME
psql -c "insert into people2(id, person_id, name) values(12, 6, 'Helen');" $DB_NAME

psql -c "insert into friends(person_id, friend_id) values(1, 2);" $DB_NAME
psql -c "insert into friends(person_id, friend_id) values(1, 3);" $DB_NAME
psql -c "insert into friends(person_id, friend_id) values(1, 4);" $DB_NAME
psql -c "insert into friends(person_id, friend_id) values(3, 4);" $DB_NAME
psql -c "insert into friends(person_id, friend_id) values(5, 6);" $DB_NAME

psql -c "insert into friends(person_id, friend_id) values(2, 1);" $DB_NAME
psql -c "insert into friends(person_id, friend_id) values(3, 1);" $DB_NAME
psql -c "insert into friends(person_id, friend_id) values(4, 1);" $DB_NAME
psql -c "insert into friends(person_id, friend_id) values(4, 3);" $DB_NAME
psql -c "insert into friends(person_id, friend_id) values(6, 5);" $DB_NAME