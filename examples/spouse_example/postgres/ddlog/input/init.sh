#!/usr/bin/env bash
# A script for loading DeepDive spouse example data into PostgreSQL database
set -eux
cd "$(dirname "$0")"

./prepare_data.sh
