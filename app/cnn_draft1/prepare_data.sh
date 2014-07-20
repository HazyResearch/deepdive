#! /bin/bash

# Configuration
export GREENPLUM_FLAGS="-h raiders3.stanford.edu -p 5432 -U amirabs"
export DBNAME=deepdive_images
DBNAME=deepdive_images
GREENPLUM_FLAGS="-h raiders3.stanford.edu -p 5432 -U amirabs"

# pg_ctl -D /usr/local/var/postgres -l /usr/local/var/postgres/server.log start

dropdb $GREENPLUM_FLAGS $DBNAME
createdb $GREENPLUM_FLAGS $DBNAME
psql $GREENPLUM_FLAGS -d $DBNAME -c "drop schema if exists public cascade; create schema public;"

psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE TABLE images(id bigint,\
										image_id bigint,\
										pixels text,\
										label int) DISTRIBUTED BY (image_id)"

psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE TABLE variables0_layer0(id bigint,\
										vector_id text,\
										image_id bigint,\
										x int,\
										y int,\
										value real,\
										fid int) DISTRIBUTED BY (image_id)"


psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE TABLE variables1_layer0(id bigint,\
										image_id bigint,\
										Bx int,\
										By int,\
										prev_ids text[],\
										fid int) DISTRIBUTED BY (image_id)"

psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE TABLE variables0_layer1(id bigint,\
										vector_id text,\
										image_id bigint,\
										x int,\
										y int,\
										value real,\
										fid int) DISTRIBUTED BY (image_id)"

psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE TABLE variables1_layer1(id bigint,\
										image_id bigint,\
										Bx int,\
										By int,\
										prev_ids text[],\
										fid int) DISTRIBUTED BY (image_id)"

psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE TABLE variables0_layer2(id bigint,\
										vector_id text,\
										image_id bigint,\
										x int,\
										y int,\
										value real,\
										fid int) DISTRIBUTED BY (image_id)"

psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE TABLE variables1_layer2(id bigint,\
										image_id bigint,\
										Bx int,\
										By int,\
										prev_ids text[],\
										fid int) DISTRIBUTED BY (image_id)"

psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE TABLE variables0_layer3(id bigint,\
										vector_id text,\
										image_id bigint,\
										x int,\
										y int,\
										value real,\
										fid int) DISTRIBUTED BY (image_id)"

psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE TABLE variables1_layer3(id bigint,\
										image_id bigint,\
										Bx int,\
										By int,\
										prev_ids text[],\
										fid int) DISTRIBUTED BY (image_id)"

psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE TABLE variables0_layer4(id bigint,\
										vector_id text,\
										image_id bigint,\
										x int,\
										y int,\
										value real,\
										fid int) DISTRIBUTED BY (image_id)"

psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE TABLE variables1_layer4(id bigint,\
										image_id bigint,\
										Bx int,\
										By int,\
										prev_ids text[],\
										fid int) DISTRIBUTED BY (image_id)"

psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE TABLE variables0_layer5(id bigint,\
										vector_id text,\
										image_id bigint,\
										x int,\
										y int,\
										value real,\
										fid int) DISTRIBUTED BY (image_id)"

psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE TABLE variables1_layer5(id bigint,\
										image_id bigint,\
										Bx int,\
										By int,\
										prev_ids text[],\
										fid int) DISTRIBUTED BY (image_id)"

psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE TABLE variables0_layer6(id bigint,\
										vector_id text,\
										image_id bigint,\
										x int,\
										y int,\
										value real,\
										fid int) DISTRIBUTED BY (image_id)"

psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE TABLE variables0_layer_lr(id bigint,\
										vector_id text,\
										image_id bigint,\
										x int,\
										y int,\
										class int,\
										value real,\
										fid int) DISTRIBUTED BY (image_id)"

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
