#! /usr/bin/env bash

HOST=zifei@whale.stanford.edu
WWW_DIR=/afs/cs/group/infolab/deepdive/www
HTML_DIR="$(dirname $0)/_site";

echo "Building site..."
jekyll build

echo "Deplying site..."
scp -r $(dirname $0)/_site/* $HOST:$WWW_DIR/