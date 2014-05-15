# This script takes one input parameter: table_name.
psql -d deepdive_spouse -c "
  TRUNCATE $1 CASCADE;
"