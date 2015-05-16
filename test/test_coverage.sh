#! /usr/bin/env bash

# Create a link for the code to current dir
ln -s src/main/scala/org ./

# Do the coverage test and push the report
sbt coveralls

# Remove the symbolic link
rm -f org
