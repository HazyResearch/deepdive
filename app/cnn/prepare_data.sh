#! /bin/bash

# Configuration

######### GREENPLUM CONFIG ###########
export GREENPLUM_FLAGS="-h raiders3.stanford.edu -p 5432 -U amirabs"
export DBNAME=deepdive_images_10
GREENPLUM_FLAGS="-h raiders3.stanford.edu -p 5432 -U amirabs"

#pg_ctl -D /usr/local/var/postgres -l /usr/local/var/postgres/server.log start

dropdb $GREENPLUM_FLAGS $DBNAME
createdb $GREENPLUM_FLAGS $DBNAME


psql $GREENPLUM_FLAGS -d $DBNAME -c "drop schema if exists public cascade; create schema public;"

psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE ORDERED AGGREGATE array_accum (anyelement) (sfunc = array_append, stype = anyarray, initcond = '{}');" 

psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE ORDERED AGGREGATE array_agg_mult (anyarray)  (SFUNC     = array_cat, STYPE     = anyarray, INITCOND  = '{}');" 


psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE TABLE image_paths(id bigint,\
										file_path text,\
										starting_id bigint) DISTRIBUTED BY (file_path)"

psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE TABLE images(id bigint,\
										image_id bigint,\
										pixels real[],\
										num_rows int,\
										num_cols int,\
										label int) DISTRIBUTED BY (image_id)"

psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE TABLE variables_layer0(id bigint,\
										image_id bigint,\
										fid int,\
										num_rows int,\
										num_cols int,\
										values real[],
										layer int) DISTRIBUTED BY (image_id)"

psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE TABLE variables_layer1(id bigint,\
										image_id bigint,\
										fid int,\
										num_rows int,\
										num_cols int,\
										values real[],
										layer int) DISTRIBUTED BY (image_id)"

psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE TABLE variables_layer2(id bigint,\
										image_id bigint,\
										fid int,\
										num_rows int,\
										num_cols int,\
										values real[],
										layer int) DISTRIBUTED BY (image_id)"

psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE TABLE variables_layer3(id bigint,\
										image_id bigint,\
										fid int,\
										num_rows int,\
										num_cols int,\
										values real[],
										layer int) DISTRIBUTED BY (image_id)"

psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE TABLE variables_layer4(id bigint,\
										image_id bigint,\
										fid int,\
										num_rows int,\
										num_cols int,\
										values real[],
										layer int) DISTRIBUTED BY (image_id)"

psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE TABLE variables_layer5(id bigint,\
										image_id bigint,\
										fid int,\
										num_rows int,\
										num_cols int,\
										values real[],
										layer int) DISTRIBUTED BY (image_id)"

psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE TABLE variables_layer6(id bigint,\
										image_id bigint,\
										fid int,\
										num_rows int,\
										num_cols int,\
										values real[],
										layer int) DISTRIBUTED BY (image_id)"

# psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE TABLE variables_layer_lr(id bigint,\
# 										image_id bigint,\
# 										fid int,\
# 										num_rows int,\
# 										num_cols int,\
# 										values real[],
# 										layer int) DISTRIBUTED BY (image_id)"
#Fid in logistic regression layer means the class of the variable


psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE TABLE conv_layer0_1(id bigint,\
										image_id bigint,\
										fid int,\
										location_x int,\
										location_y int,\
										size int,\
										center_fids int[],\
										center_locations_x int[],\
										center_locations_y int[]) DISTRIBUTED BY (image_id)"

psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE TABLE conv_layer1_2(id bigint,\
										image_id bigint,\
										fid int,\
										location_x int,\
										location_y int,\
										size int,\
										center_fids int[],\
										center_locations_x int[],\
										center_locations_y int[]) DISTRIBUTED BY (image_id)"

psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE TABLE conv_layer2_3(id bigint,\
										image_id bigint,\
										fid int,\
										location_x int,\
										location_y int,\
										size int,\
										center_fids int[],\
										center_locations_x int[],\
										center_locations_y int[]) DISTRIBUTED BY (image_id)"

psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE TABLE conv_layer3_4(id bigint,\
										image_id bigint,\
										fid int,\
										location_x int,\
										location_y int,\
										size int,\
										center_fids int[],\
										center_locations_x int[],\
										center_locations_y int[]) DISTRIBUTED BY (image_id)"

psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE TABLE conv_layer4_5(id bigint,\
										image_id bigint,\
										fid int,\
										location_x int,\
										location_y int,\
										size int,\
										center_fids int[],\
										center_locations_x int[],\
										center_locations_y int[]) DISTRIBUTED BY (image_id)"

psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE TABLE conv_layer5_6(id bigint,\
										image_id bigint,\
										fid int,\
										location_x int,\
										location_y int,\
										size int,\
										center_fids int[],\
										center_locations_x int[],\
										center_locations_y int[]) DISTRIBUTED BY (image_id)"

# psql $GREENPLUM_FLAGS -d $DBNAME -c "CREATE TABLE conv_layer6_lr(id bigint,\
# 										image_id bigint,\
# 										fid int,\
# 										location_x int,\
# 										location_y int,\
# 										size int,\
# 										center_fids int[],\
# 										center_locations_x int[],\
# 										center_locations_y int[]) DISTRIBUTED BY (image_id)"


