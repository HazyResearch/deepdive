#! /bin/bash

# Configuration
DB_NAME=deepdive_smoke

cd `dirname $0`
BASE_DIR=`pwd`

dropdb -p $PGPORT -h $PGHOST deepdive_smoke
createdb -p $PGPORT -h $PGHOST deepdive_smoke

psql -p $PGPORT -h $PGHOST $DBNAME -c "DROP SCHEMA IF EXISTS public CASCADE; CREATE SCHEMA public;"

psql -p $PGPORT -h $PGHOST $DBNAME -c """
	DROP TABLE IF EXISTS people1 CASCADE;
	DROP TABLE IF EXISTS people2 CASCADE;
	DROP TABLE IF EXISTS friends CASCADE;

	CREATE TABLE people1 (
		id bigserial primary key,
		person_id bigint,
		name text,
		has_cancer boolean
	)

	CREATE TABLE people2 (
		id bigserial primary key,
		person_id bigint,
		name text,
		smokes boolean
	)

	CREATE TABLE friends (
		id bigserial primary key,
		person_id bigint,
		friend_id bigint
	)
;"""

psql -p $PGPORT -h $PGHOST $DBNAME -c """
	INSERT INTO people1(id, person_id, name) VALUES
		(1, 1, 'Anna'),
		(2, 2, 'Bob'),
		(3, 3, 'Edward'),
		(4, 4, 'Frank'),
		(5, 5, 'Gary'),
		(6, 6, 'Helen')
;"""

psql -p $PGPORT -h $PGHOST $DBNAME -c """
	INSERT INTO people2(id, person_id, name, smokes) VALUES
		(7, 1, 'Anna', TRUE),
		(8, 2, 'Bob', NULL),
		(9, 3, 'Edward', TRUE),
		(10, 4, 'Frank', NULL),
		(11, 5, 'Gary', NULL),
		(12, 6, 'Helen', NULL)
;"""

psql -p $PGPORT -h $PGHOST $DBNAME -c """
	INSERT INTO friends(person_id, friend_id) VALUES 
		(1, 2), (1, 3), (1, 4), (3, 4), (5, 6), (2, 1), (3, 1), (4, 1), (4, 3), (6, 5)
;"""