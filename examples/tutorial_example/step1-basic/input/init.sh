#!/usr/bin/env bash
# A script for initializing database for the spouse example in DeepDive's walk-through
set -eux
cd "$(dirname "$0")"  # move into the directory where this script is

# download and unzip the input dataset if necessary
if ! [[ -e deepdive-tutorial-data.zip ]]; then
    curl -RLO http://i.stanford.edu/hazy/deepdive-tutorial-data.zip
    unzip deepdive-tutorial-data.zip
fi

# load the data into database
deepdive sql execute "COPY sentences FROM STDIN CSV" <./sentences_dump.csv
