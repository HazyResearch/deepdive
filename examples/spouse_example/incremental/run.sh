#! /bin/bash

export PIPELINE=$2
if [[ $3 == "inc" ]]; then
  sbt "run -c $APP_HOME/$1"
else
  sbt "run -c $APP_HOME/$1 -o $BASEDIR"
fi
