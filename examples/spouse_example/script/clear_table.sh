# This script takes one input parameter: table_name.
psql -d $DBNAME -c "
  TRUNCATE $1 CASCADE;
"