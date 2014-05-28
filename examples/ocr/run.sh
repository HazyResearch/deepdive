#! /bin/bash

. "$(dirname $0)/env.sh"

export APP_HOME=`cd $(dirname $0); pwd`
export DEEPDIVE_HOME=`cd $(dirname $0)/../..; pwd`

cd $APP_HOME/data/raw/
python gen_feature_table.py

cd $DEEPDIVE_HOME

bash $APP_HOME/prepare_data.sh
deepdive -c $APP_HOME/application.conf
