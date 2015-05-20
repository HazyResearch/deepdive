#! /usr/bin/env bash

# Create a symbolic link at src/main/scala/org to let coverage test retrieve all source,
# and remove it after test
# Ref: https://github.com/scoverage/sbt-coveralls
ln -s src/main/scala/org ./
sbt coveralls
rm -f org