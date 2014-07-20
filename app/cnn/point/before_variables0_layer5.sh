#! /usr/bin/env bash
DBNAME=deepdive_images

psql -c "TRUNCATE variables_layer5 CASCADE;" deepdive_images


