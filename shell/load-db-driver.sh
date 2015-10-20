#!/usr/bin/env bash
# load-db-driver.sh -- Loads the corresponding database driver for the DeepDive app's database URL
# > DEEPDIVE_DB_URL=...
# > . load-db-driver.sh
#
# $DEEPDIVE_DB_URL environment variable has precedence over the db.url file in
# the DeepDive application.
##
eval "$(

# parse URL for database
url=${DEEPDIVE_DB_URL:-$(
    DEEPDIVE_APP=$(find-deepdive-app)
    export DEEPDIVE_APP
    cat "$DEEPDIVE_APP"/db.url
)}

# recognize the database type from the URL scheme
dbtype=${url%%://*}

# place the db-driver on PATH
PATH="$DEEPDIVE_HOME"/util/db-driver/"$dbtype":"$PATH"
# make sure all operations are defined
for op in parse init execute query load
do type db-$op &>/dev/null || error "db-$op operation not available for $dbtype"
done

# default environment setup script
echo "DEEPDIVE_DB_URL=${url}"
echo 'PATH="$DEEPDIVE_HOME"/util/db-driver/'"${dbtype}"':"$PATH"'
echo "export DEEPDIVE_DB_URL PATH"

# parse the URL and print necessary environment setup script
db-parse "$url"

)"
