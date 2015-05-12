#! /usr/bin/env bash

if [ $# = 1 ]; then
  export DBNAME=$1
else
  echo "Usage: bash setup_database DBNAME"
  DBNAME=deepdive_spouse
fi

DBS=$(impala-shell -i localhost -B --quiet -q "SHOW DATABASES LIKE \"$DBNAME\"")
echo $DBS
if [ "$DBS" = '' ]; then
  exit 0
fi

TABLES=$(impala-shell -i localhost -d $DBNAME -B --quiet -q "SHOW TABLES")
for line in $TABLES 
do
  impala-shell -i localhost -d $DBNAME --quiet -q "DROP TABLE $line"
done

impala-shell -i localhost --quiet -q "DROP DATABASE IF EXISTS $DBNAME"

