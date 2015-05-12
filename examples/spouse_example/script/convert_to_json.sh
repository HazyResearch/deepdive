#!/bin/sh

# the original file can only be read in postgresql.
# we use postgresql (psql) for conversion

# first run 

cd ..

. ./setup_database.sh

psql -t -q -d $DBNAME -c 'select row_to_json(sentences) from sentences' -o data/sentences_dump.json

psql -t -q -d $DBNAME -c 'select row_to_json(articles) from articles' -o data/articles_dump.json
