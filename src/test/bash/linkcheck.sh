#! /usr/bin/env bash

export DEEPDIVE_HOME=`cd $(dirname $0)/../../../; pwd`

# Run linkchecker and send output to the log file
linkchecker http://deepdive.stanford.edu/ >$DEEPDIVE_HOME/linkchecker_errorlog.txt

# Look for string "Error" in the output log
line=$(grep Error $DEEPDIVE_HOME/linkchecker_errorlog.txt)

if [ $? -eq 0 ]; then
  echo "[`date`] Errors found:"
  cat $DEEPDIVE_HOME/linkchecker_errorlog.txt
  rm -f $DEEPDIVE_HOME/linkchecker_errorlog.txt
  echo "[FAILED] website link checking test failed!"
  exit 1;
else
  echo "[`date`] No errors found."
  rm -f $DEEPDIVE_HOME/linkchecker_errorlog.txt
fi
