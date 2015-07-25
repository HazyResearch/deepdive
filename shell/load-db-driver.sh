#!/usr/bin/env bash
# load-db-driver.sh -- Loads the corresponding database driver for the DeepDive app's database URL
# > cd $APP_HOME
# > DEEPDIVE_DB_URL=...
# > . load-db-driver.sh
#
# $DEEPDIVE_DB_URL environment variable has precedence over the db.url file in
# the DeepDive application.
##
eval "$(

# parse URL for database
url=${DEEPDIVE_DB_URL:-$(cat db.url)}

# recognize the database type from the URL scheme
dbtype=${url%%://*}

# place the driver on PATH
PATH="$DEEPDIVE_HOME"/util/driver."$dbtype":"$PATH"
# make sure all operations are defined
for op in parse init execute query
do type db-$op &>/dev/null || error "db-$op operation not available for $dbtype"
done

# default environment setup script
echo "DEEPDIVE_DB_URL=${url}"
echo 'PATH="$DEEPDIVE_HOME"/util/driver.'"${dbtype}"':"$PATH"'
echo "export DEEPDIVE_DB_URL PATH"

# parse the URL and print necessary environment setup script
db-parse "$url"

)"
