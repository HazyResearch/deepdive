#!/usr/bin/env bash
# load-db-driver.sh -- Loads the corresponding database driver for the DeepDive app's database URL
# > DEEPDIVE_DB_URL=...
# > . load-db-driver.sh
#
# $DEEPDIVE_DB_URL environment variable has precedence over the db.url file in
# the DeepDive application.
##
eval "$(
set -eu
trap 'echo false' ERR

# parse URL for database
url=${DEEPDIVE_DB_URL:-}
if [[ -z "$url" ]]; then
    DEEPDIVE_APP=$(find-deepdive-app)
    export DEEPDIVE_APP
    url=$(eval echo "$(cat "$DEEPDIVE_APP"/db.url)")
fi

# recognize the database type from the URL scheme
dbtype=${url%%://*}

# place the db-driver on PATH
PATH="$DEEPDIVE_HOME"/util/db-driver/"$dbtype":"$PATH"
# make sure all operations are defined
for op in \
    parse \
    init \
    execute \
    query \
    prompt \
    create-table \
    create-table-def \
    create-table-as \
    create-table-like \
    create-view-as \
    load \
    unload \
    analyze \
    create_calibration_view \
    assign_sequential_id \
    generate_series \
    ;
do type db-$op &>/dev/null || error "db-$op operation not available for $dbtype"
done

# default environment setup script
escape4sh export DEEPDIVE_DB_URL="${url}"
escape4sh export PATH="$DEEPDIVE_HOME"/util/db-driver/"${dbtype}":"$PATH"

# parse the URL and print necessary environment setup script
db-parse "$url"

)"
