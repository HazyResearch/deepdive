# Usage: fill_sequence.sh  TABLE_NAME  COLUMN_NAME
psql -c """
        DROP SEQUENCE IF EXISTS tmp_sequence_$1;
        CREATE SEQUENCE tmp_sequence_$1;
        UPDATE $1 SET $2 = nextval('tmp_sequence_$1');
""" $DBNAME