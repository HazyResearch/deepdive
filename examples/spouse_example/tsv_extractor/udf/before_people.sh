#! /usr/bin/env bash

psql -c "TRUNCATE people_mentions CASCADE;" $DBNAME