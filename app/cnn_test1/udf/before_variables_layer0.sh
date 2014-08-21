#! /usr/bin/env bash
psql -c "TRUNCATE variables_layer0 CASCADE;" $DBNAME

