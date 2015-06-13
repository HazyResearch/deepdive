#!/usr/bin/env bash
set -e

# FIXME this LD_LIBRARY_PATH config for sampler that plagues every part of our code as well as users' application code should be completely encapsulated within a wrapper command.
# XXX For now, there's no easy way to unbreak the tests other than duplicating this everywhere.
export DEEPDIVE_HOME=$(cd "$(dirname "$0")" && pwd)
case $(uname) in
  Darwin)
    export LD_LIBRARY_PATH=$DEEPDIVE_HOME/lib/dw_mac/lib/protobuf/lib:$DEEPDIVE_HOME/lib/dw_mac/lib:$LD_LIBRARY_PATH
    export DYLD_LIBRARY_PATH=$DEEPDIVE_HOME/lib/dw_mac:$DYLD_LIBRARY_PATH
    ;;

  Linux*)
    export LD_LIBRARY_PATH=$DEEPDIVE_HOME/lib/dw_linux/lib:$DEEPDIVE_HOME/lib/dw_linux/lib64:$DEEPDIVE_HOME/lib/dw_linux/lib/numactl-2.0.9/:$LD_LIBRARY_PATH
    ;;

  *)
    echo >&2 "$(uname): Unsupported OS"
    false
esac

# uncompress all test and example data
echo "Preparing test data..."
examples/spouse_example/prepare_data.sh

# if command psql exist, then test psql
if hash psql 2>/dev/null; then
  echo "Testing psql..."
  bash test/test_psql.sh
fi

# if command mysql exist, then test mysql
if hash mysql 2>/dev/null; then
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

