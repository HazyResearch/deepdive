#! /usr/bin/env bash
psql -c "TRUNCATE sentences CASCADE;" $DBNAME
