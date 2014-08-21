#! /usr/bin/env bash
DBNAME=deepdive_images

psql -c "TRUNCATE variables_layer4 CASCADE;" deepdive_images


