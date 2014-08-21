#! /bin/bash

# Configuration
export DBNAME=deepdive_images
DBNAME=deepdive_images

pg_ctl -D /usr/local/var/postgres -l /usr/local/var/postgres/server.log start

dropdb $DBNAME
createdb $DBNAME
psql -d $DBNAME -c "drop schema if exists public cascade; create schema public;"

psql -d $DBNAME -c "CREATE TABLE images(id bigint,\
										image_id bigint,\
										pixels text,\
										label int)"

psql -d $DBNAME -c "CREATE TABLE variables0_layer0(id bigint,\
										vector_id text,\
										image_id bigint,\
										x int,\
										y int,\
										value real,\
										fid int)"

psql -d $DBNAME -c "CREATE TABLE variables1_layer0(id bigint,\
										image_id bigint,\
										Bx int,\
										By int,\
										prev_ids text[],\
										fid int)"

psql -d $DBNAME -c "CREATE TABLE variables0_layer1(id bigint,\
										vector_id text,\
										image_id bigint,\
										x int,\
										y int,\
										value real,\
										fid int)"

psql -d $DBNAME -c "CREATE TABLE variables1_layer1(id bigint,\
										image_id bigint,\
										Bx int,\
										By int,\
										prev_ids text[],\
										fid int)"

psql -d $DBNAME -c "CREATE TABLE variables0_layer2(id bigint,\
										vector_id text,\
										image_id bigint,\
										x int,\
										y int,\
										value real,\
										fid int)"

# psql -U $DB_USER -c "CREATE TABLE variables_layer1(id bigserial primary key, \
# 													image_id bigserial,      \
# 													x int,                   \
# 													y int,                   \
# 													variables integer[],     \
# 													value int)"           $DBNAME

# psql -U $DB_USER -c "CREATE TABLE variables_layer2(id bigserial primary key, \
# 													image_id bigserial,      \
# 													x int,                   \
# 													y int,                   \
# 													variables integer[],          \
# 													value int)"           $DBNAME

# psql -U $DB_USER -c "CREATE TABLE variables_layer3(id bigserial primary key, \
# 													image_id bigserial,      \
# 													x int,                   \
# 													y int,                   \
# 													variables integer[],          \
# 													value int)"           $DBNAME

# psql -U $DB_USER -c "CREATE TABLE variables_layer4(id bigserial primary key, \
# 													image_id bigserial,      \
# 													x int,                   \
# 													y int,                   \
# 													variables integer[],          \
# 													value int)"           $DBNAME

# psql -U $DB_USER -c "CREATE TABLE variables_layer5(id bigserial primary key, \
# 													image_id bigserial,      \
# 													label int,               \
# 													variables integer[],          \
# 													value bool)"           $DBNAME
