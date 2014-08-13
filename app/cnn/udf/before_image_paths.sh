#! /usr/bin/env bash
psql -c "TRUNCATE image_paths CASCADE;" $DBNAME

