#! /usr/bin/env bash
DBNAME=deepdive_images

psql -c "TRUNCATE variables_layer_lr CASCADE;" deepdive_images
