# Usage: fill_sequence.sh  TABLE_NAME  COLUMN_NAME
# Postgres will use a random continuous sequence to fill your column starting from 1. The order is not guaranteed.
psql -c "
        DROP SEQUENCE IF EXISTS tmp_sequence_$1;
        CREATE SEQUENCE tmp_sequence_$1;
        UPDATE $1 SET $2 = nextval('tmp_sequence_$1');
        DROP SEQUENCE tmp_sequence_$1;
" $DBNAME