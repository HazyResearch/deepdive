#! /bin/bash

# Configuration

######### GREENPLUM CONFIG ###########
export GREENPLUM_FLAGS="-h raiders3.stanford.edu -p 5432 -U amirabs"
export DBNAME=deepdive_images_local
DBNAME=deepdive_images_local
GREENPLUM_FLAGS="-h raiders3.stanford.edu -p 5432 -U amirabs"

#pg_ctl -D /usr/local/var/postgres -l /usr/local/var/postgres/server.log start

dropdb $GREENPLUM_FLAGS $DBNAME
createdb $GREENPLUM_FLAGS $DBNAME


psql $GREENPLUM_FLAGS -d $DBNAME -c "drop schema if exists public cascade; create schema public;"

psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE ORDERED AGGREGATE array_accum (anyelement) (sfunc = array_append, stype = anyarray, initcond = '{}');" 

psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE TABLE images(id bigint,\
										image_id bigint,\
										pixels text,\
										label int) DISTRIBUTED BY (image_id)"

psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE TABLE variables_layer0(id bigint,\
										vector_id bigint,\
										image_id bigint,\
										x int,\
										y int,\
										value real,\
										fid int) DISTRIBUTED BY (image_id)"

psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE TABLE variables_layer1(id bigint,\
										vector_id bigint,\
										image_id bigint,\
										x int,\
										y int,\
										prev_ids bigint[],\
										value real,\
										fid int) DISTRIBUTED BY (image_id)"


psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE TABLE variables_layer2(id bigint,\
										vector_id bigint,\
										image_id bigint,\
										x int,\
										y int,\
										prev_ids bigint[],\
										value real,\
										fid int) DISTRIBUTED BY (image_id)"

psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE TABLE variables_layer3(id bigint,\
										vector_id bigint,\
										image_id bigint,\
										x int,\
										y int,\
										prev_ids bigint[],\
										value real,\
										fid int) DISTRIBUTED BY (image_id)"

psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE TABLE variables_layer4(id bigint,\
										vector_id bigint,\
										image_id bigint,\
										x int,\
										y int,\
										prev_ids bigint[],\
										value real,\
										fid int) DISTRIBUTED BY (image_id)"

psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE TABLE variables_layer5(id bigint,\
										vector_id bigint,\
										image_id bigint,\
										x int,\
										y int,\
										prev_ids bigint[],\
										value real,\
										fid int) DISTRIBUTED BY (image_id)"

psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE TABLE variables_layer6(id bigint,\
										vector_id bigint,\
										image_id bigint,\
										x int,\
										y int,\
										prev_ids bigint[],\
										value real,\
										fid int) DISTRIBUTED BY (image_id)"

psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE TABLE variables_layer_lr(id bigint,\
										vector_id bigint[],\
										image_id bigint,\
										x int,\
										y int,\
										class int,\
										value real,\
										fid int) DISTRIBUTED BY (image_id)"