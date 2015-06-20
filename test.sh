#!/usr/bin/env bash
set -e

# make sure deepdive command is on PATH
PATH="$(dirname "$0")"/shell:"$PATH"

# uncompress all test and example data
echo "Preparing test data..."
examples/spouse_example/data/prepare_data.sh

# if command psql exist, then test psql
if hash psql 2>/dev/null; then
  echo "Testing psql..."
  bash test/test_psql.sh
fi

# if command mysql exist, then test mysql
if false && hash mysql 2>/dev/null; then
  echo "Testing mysql..."
  bash test/test_mysql.sh
fi

# if greenplum supported (gpfdist exist), then test GP
if hash gpfdist 2>/dev/null; then
  echo "Testing Greenplum..."
  bash test/test_gp.sh
fi

echo "Testing incremental support..."
test/test_incremental.sh

echo "Testing Other modules..."
bash src/test/python/test_ddext/test.sh

