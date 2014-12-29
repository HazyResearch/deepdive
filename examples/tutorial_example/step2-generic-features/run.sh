#! /bin/bash

# Check the data files
if ! [ -d data ] \
  || ! [ -f data/spouses.tsv ] \
  || ! [ -f data/non-spouses.tsv ] \
  || ! [ -f data/sentences_dump.csv ] \
  || ! [ -f data/sentences_dump_large.csv ]; then
  echo "ERROR: Data files do not exist. You should download the data from http://i.stanford.edu/hazy/deepdive-tutorial-data.zip, and extract files to data/ directory."
  exit 1;
fi

export APP_HOME=`cd $(dirname $0)/; pwd`
export DEEPDIVE_HOME=`cd $(dirname $0)/../../../; pwd`

# Database Configuration
export DBNAME=deepdive_spouse

export PGUSER=${PGUSER:-`whoami`}
export PGPASSWORD=${PGPASSWORD:-}
export PGPORT=${PGPORT:-5432}
export PGHOST=${PGHOST:-localhost}

# Initialize database
#bash $APP_HOME/setup_database.sh $DBNAME

# Using ddlib
export PYTHONPATH=$DEEPDIVE_HOME/ddlib:$PYTHONPATH

cd $DEEPDIVE_HOME

# Run DeepDive
set -e
# SBT_OPTS="-Xmx4g" sbt "run -c $APP_HOME/application.conf"
deepdive -c $APP_HOME/application.conf

# Generate automatic reports
cd $APP_HOME
braindump
