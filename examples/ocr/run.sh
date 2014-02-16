#! /bin/bash

export DBNAME=deepdive_ocr
export PGUSER=${PGUSER:-`whoami`}
export PGPASSWORD=${PGPASSWORD:-}
export PGPORT=${PGPORT:-5432}
export PGHOST=${PGHOST:-localhost}

cd data/raw/
ROOT_PATH=`pwd`
python gen_feature_table.py

cd "$(dirname $0)/../../../../";
ROOT_PATH=`pwd`

$ROOT_PATH/examples/ocr/prepare_data.sh
env SBT_OPTS="-Xmx4g" sbt "run -c examples/ocr/application.conf"

cd $ROOT_PATH/examples/ocr/
ROOT_PATH=`pwd`
python feature-analysis.py
