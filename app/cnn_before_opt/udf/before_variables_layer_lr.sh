#! /usr/bin/env bash
psql -c "TRUNCATE variables_layer_lr CASCADE;" $DBNAME
