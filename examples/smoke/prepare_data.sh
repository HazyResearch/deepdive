#! /bin/bash

# Configuration
DB_NAME=deepdive_smoke
DB_USER=
DB_PASSWORD=

cd `dirname $0`
BASE_DIR=`pwd`

dropdb deepdive_smoke
createdb deepdive_smoke

psql -c "drop schema if exists public cascade; create schema public;" $DB_NAME

psql -c "create table people(id bigserial primary key, name text, has_cancer boolean, smokes boolean);" $DB_NAME
psql -c "create table friends(id1 bigserial references people(id), id2 bigserial references people(id), are_friends boolean, primary key(id1, id2));" $DB_NAME

psql -c "insert into people(id, name, smokes) values (1, 'Anna', TRUE);" $DB_NAME
psql -c "insert into people(id, name) values (2, 'Bob');" $DB_NAME
psql -c "insert into people(id, name, smokes) values(3, 'Edward', TRUE);" $DB_NAME
psql -c "insert into people(id, name) values(4, 'Frank');" $DB_NAME
psql -c "insert into people(id, name) values(5, 'Gary');" $DB_NAME
psql -c "insert into people(id, name) values(6, 'Helen');" $DB_NAME

psql -c "insert into friends(id1, id2, are_friends) values(1, 2, TRUE);" $DB_NAME
psql -c "insert into friends(id1, id2, are_friends) values(1, 3, TRUE);" $DB_NAME
psql -c "insert into friends(id1, id2, are_friends) values(1, 4, TRUE);" $DB_NAME
psql -c "insert into friends(id1, id2, are_friends) values(3, 4, TRUE);" $DB_NAME
psql -c "insert into friends(id1, id2, are_friends) values(5, 6, TRUE);" $DB_NAME
psql -c "insert into friends(id1, id2, are_friends) values(5, 4, FALSE);" $DB_NAME

psql -c "insert into friends(id1, id2, are_friends) values(2, 1, TRUE);" $DB_NAME
psql -c "insert into friends(id1, id2, are_friends) values(3, 1, TRUE);" $DB_NAME
psql -c "insert into friends(id1, id2, are_friends) values(4, 1, TRUE);" $DB_NAME
psql -c "insert into friends(id1, id2, are_friends) values(4, 3, TRUE);" $DB_NAME
psql -c "insert into friends(id1, id2, are_friends) values(6, 5, TRUE);" $DB_NAME
psql -c "insert into friends(id1, id2, are_friends) values(4, 5, FALSE);" $DB_NAME