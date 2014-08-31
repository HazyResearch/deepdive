#! /bin/bash

# read mln configuration file

# set environmental variables
. "$(dirname $0)/env_conf.sh"

# set BASE_DIR
cd `dirname $0`
BASE_DIR=`pwd`

# create the database
dropdb -p $PGPORT -h $PGHOST $DBNAME
createdb -p $PGPORT -h $PGHOST $DBNAME
psql -p $PGPORT -h $PGHOST $DBNAME -c """
CREATE AGGREGATE array_accum (anyelement)
(
    sfunc = array_append,
    stype = anyarray,
    initcond = '{}'
);
"""

# set ROOT_PATH
cd "$(dirname $0)/../.."
ROOT_PATH=`pwd`

# generate mln_auto.conf
echo 'Generating mln_auto.conf'
rm -f $BASE_DIR/mln_auto.conf
echo 'db_url = jdbc:postgresql://'$PGHOST':'$PGPORT'/'$DBNAME >> $BASE_DIR/mln_auto.conf
echo 'db_username = '$PGUSER >> $BASE_DIR/mln_auto.conf
echo 'db_password = '$PGPASSWORD >> $BASE_DIR/mln_auto.conf
echo 'dir_working = /tmp/deepdive_mln_workspace' >> $BASE_DIR/mln_auto.conf

# build up database and generate application_auto.conf
rm -f application_auto.conf
java -jar $ROOT_PATH/mln/tuffy.jar -db public -i $BASE_DIR/prog.mln -e $BASE_DIR/evidence.db -queryFile $BASE_DIR/query.db -r $BASE_DIR/application_auto.conf -keepData -conf $BASE_DIR/mln_auto.conf -learnwt

# run Deepdive
env SBT_OPTS="-Xmx4g" $ROOT_PATH/sbt/sbt "run -c "$BASE_DIR/application_auto.conf
