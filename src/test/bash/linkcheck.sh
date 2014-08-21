#! /usr/bin/env bash

DEEPDIVE_HOME=`cd $(dirname $0)/../../../; pwd`
# URL_TO_CHECK=http://localhost:4000/
URL_TO_CHECK=http://deepdive.stanford.edu/

# Run linkchecker and send output to the log file
linkchecker --check-extern $URL_TO_CHECK >$DEEPDIVE_HOME/linkchecker_errorlog.txt

# Look for string "Error" in the output log
line=$(grep Error $DEEPDIVE_HOME/linkchecker_errorlog.txt)

if [ $? -eq 0 ]; then
  echo "[`date`] Errors found:"
  cat $DEEPDIVE_HOME/linkchecker_errorlog.txt
  echo "[FAILED] website link checking test failed!"
  exit 1;
else
  echo "[`date`] No errors found."
  rm -f $DEEPDIVE_HOME/linkchecker_errorlog.txt
fi
