#! /bin/bash

: ${DEEPDIVE_ACTIVE_INCREMENTAL_VARIABLES:?names of active variable tables must be specified}
: ${DEEPDIVE_ACTIVE_INCREMENTAL_RULES:?names of active inference rules must be specified}

# generate active variables
for i in $DEEPDIVE_ACTIVE_INCREMENTAL_VARIABLES; do
  echo Generating active variables from $i...
  tmpout=$BASEDIR/dd_active_vars_$i
  psql -d $DBNAME -c "COPY (SELECT id FROM $i) TO STDOUT;" > $tmpout
  format_converter active $tmpout
  rm $tmpout
done
cat $BASEDIR/dd_active_vars_*.bin > $BASEDIR/active.variables
rm $BASEDIR/dd_active_vars_*.bin

# active factors
for i in $DEEPDIVE_ACTIVE_INCREMENTAL_RULES; do
  echo Generating active factors from $i...
  tmpout=$BASEDIR/dd_active_factors_$i
  psql -d $DBNAME -c "COPY (SELECT id FROM dd_query_$i) TO STDOUT;" > $tmpout
  format_converter active $tmpout
  rm $tmpout
done
cat $BASEDIR/dd_active_factors_*.bin > $BASEDIR/active.factors
rm $BASEDIR/dd_active_factors_*.bin

# active weights
for i in $DEEPDIVE_ACTIVE_INCREMENTAL_RULES; do
  echo Generating active weights from $i...
  tmpout=$BASEDIR/dd_active_weights_$i
  psql -d $DBNAME -c "COPY (SELECT id FROM dd_weights_$i) TO STDOUT;" > $tmpout
  format_converter active $tmpout
  rm $tmpout
done
cat $BASEDIR/dd_active_weights_*.bin > $BASEDIR/active.weights
rm $BASEDIR/dd_active_weights_*.bin
