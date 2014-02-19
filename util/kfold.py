#! /usr/bin/env python

# Usage: kfold.py KFOLD_NUM KFOLD_ITER temporal_table_to_fold query_col_1 [query_col_2 ...]
# KFOLD_NUM (K): number of folds to do evaluation
# KFOLD_ITER (i): current number of iterations. range: 1..KFOLD_NUM

# Functionality: 
  # In temporal_table_to_fold,
  # For rows in fraction [ (i-1)/K, (i)/K ), e.g. 0%--25% for K=4, i=1,
  # Columns specified in query_col_1 [, query_col_2 ... ] 
  # will be DELETED. 

# Should be used as AFTER function of an extractor that generates temporal_table_to_fold.

# Works better with a run script with iterations, changing input KFOLD_ITER.

# e.g.: after: ${APP_HOME}"/udf/kfold.py "${KFOLD_NUM}" "${KFOLD_ITER}" filtered_labels label_t label_c"

import os, sys

# DEBUG
print '-------- KFOLD INPUT ARGS: --------\n', sys.argv

if len(sys.argv) >= 5:
  KFOLD_NUM = sys.argv[1]
  KFOLD_ITER = sys.argv[2]
  table = sys.argv[3]
  remove_vars = sys.argv[4:]

else:
  print 'Usage:',sys.argv[0],'KFOLD_NUM KFOLD_ITER temporal_table_to_fold query_col_1 query_col_2 ...'
  sys.exit(1)

sql_queries = []
numrows = 'select count(*) from ' + table


# K-fold query: hold out columns with ID from (i-1)/K to i/K
updatequery = ' '.join(
  ['UPDATE', table, 'SET', 
   ','.join([var + '= NULL' for var in remove_vars]),
   'where id < ('+ numrows + ') * ' + KFOLD_ITER + ' / '
   + KFOLD_NUM + ' and id >= ('+ numrows + ') * (' + 
    KFOLD_ITER + ' - 1) / '+ KFOLD_NUM + ';'])

sql_queries.append(updatequery)
fullquery = 'psql -c """'+'\n'.join(sql_queries)+'""" '+os.environ['DBNAME']
os.system(fullquery)

# DEBUG
print "-------- KFOLD QUERY --------\n", fullquery

