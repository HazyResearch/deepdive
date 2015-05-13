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
  impala-shell -i localhost -d $DBNAME --quiet -q "DROP TABLE IF EXISTS $line"
  impala-shell -i localhost -d $DBNAME --quiet -q "DROP VIEW IF EXISTS $line"
done

FUNCTIONS=$(impala-shell -i localhost -d $DBNAME -B --quiet -q "SHOW FUNCTIONS")
IFS=$'\n' #make newline the only separator
for line in $FUNCTIONS
do
  name=$(echo $line | cut -f 2)  #split line on tab and take second column
  impala-shell -i localhost -d $DBNAME --quiet -q "DROP FUNCTION $name"
done


impala-shell -i localhost --quiet -q "DROP DATABASE IF EXISTS $DBNAME"

