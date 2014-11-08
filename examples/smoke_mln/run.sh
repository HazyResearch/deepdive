#! /bin/bash

# check argument
if [ $# -ne 1 ] || ([ "$1" != "-marginal" ] && [ "$1" != "-learnwt" ]); then
	echo "Please use \"-marginal\" or \"-learnwt\" as argument."
	exit
fi

# set environmental variables
. "$(dirname $0)/env_conf.sh"

# set BASE_DIR
cd `dirname $0`
BASE_DIR=`pwd`

# create the database
dropdb -p $PGPORT -h $PGHOST $DBNAME
createdb -p $PGPORT -h $PGHOST $DBNAME

# set ROOT_PATH
cd "$(dirname $0)/../.."
ROOT_PATH=`pwd`

# generate mln_auto.conf
echo '>>> Generating mln_auto.conf'
rm -f $BASE_DIR/mln_auto.conf
echo 'db_url = jdbc:postgresql://'$PGHOST':'$PGPORT'/'$DBNAME >> $BASE_DIR/mln_auto.conf
echo 'db_username = '$PGUSER >> $BASE_DIR/mln_auto.conf
echo 'db_password = '$PGPASSWORD >> $BASE_DIR/mln_auto.conf
echo 'dir_working = '$BASE_DIR'/tmp' >> $BASE_DIR/mln_auto.conf

# build up database and generate application_auto.conf
rm -f application_auto.conf
java -jar $ROOT_PATH/mln/tuffy.jar -db public -i $BASE_DIR/prog.mln -e $BASE_DIR/evidence.db -queryFile $BASE_DIR/query.db -r $BASE_DIR/application_auto.conf -keepData -conf $BASE_DIR/mln_auto.conf $1

# run Deepdive
env SBT_OPTS="-Xmx4g" $ROOT_PATH/sbt/sbt "run -c "$BASE_DIR/application_auto.conf
