#! /usr/bin/env bash

if [ $# = 1 ]; then
  export DBNAME=$1
else
  echo "Usage: bash setup_database DBNAME"
  DBNAME=deepdive_spouse
fi

bash ./drop_database_impala.sh
impala-shell -i localhost --quiet -q "CREATE DATABASE $DBNAME"

export APP_HOME=`cd $(dirname $0)/; pwd`

# convert from json to tsv (without arrays)
cd $APP_HOME/script
python3 ./convert_json_to_impala_tsv.py $APP_HOME/data/articles_dump.json > $APP_HOME/data/articles_dump_impala.tsv
python3 ./convert_json_to_impala_tsv.py $APP_HOME/data/sentences_dump.json > $APP_HOME/data/sentences_dump_impala.tsv

# copy tables to hdfs
sudo -u hdfs hdfs dfs -mkdir -p /dd
sudo -u hdfs hdfs dfs -chown impala /dd
sudo -u hdfs hdfs dfs -rm -r -f -skipTrash /dd/articles_dump_impala.tsv
sudo -u hdfs hdfs dfs -rm -r -f -skipTrash /dd/sentences_dump_impala.tsv
sudo -u impala hdfs dfs -copyFromLocal $APP_HOME/data/articles_dump_impala.tsv /dd/articles_dump_impala.tsv
sudo -u impala hdfs dfs -copyFromLocal $APP_HOME/data/sentences_dump_impala.tsv /dd/sentences_dump_impala.tsv

impala-shell -i localhost -d $DBNAME --quiet -f $APP_HOME/schema_impala.sql
impala-shell -i localhost -d $DBNAME --quiet -q "load data inpath '/dd/articles_dump_impala.tsv' into table articles;"
impala-shell -i localhost -d $DBNAME --quiet -q "load data inpath '/dd/sentences_dump_impala.tsv' into table sentences;"

