#! /usr/bin/env bash

HOST=zifei@corn
WWW_DIR=/afs/.ir.stanford.edu/users/z/i/zifei/WWW/deepdive
HTML_DIR="$(dirname $0)/_site";

echo "Building site..."
jekyll build --config _config-zifei.yml

echo "Deplying site..."
scp -r $(dirname $0)/_site/* $HOST:$WWW_DIR/