set -e

# if greenplum supported (gpfdist exist), then test GP
if hash gpfdist 2>/dev/null; then
  echo "Testing Greenplum..."
  bash test/test_gp.sh $1
# if command psql exist, then test psql
elif hash psql 2>/dev/null; then
  echo "Testing psql..."
  bash test/test_psql.sh $1
fi

# if command mysql exist, then test mysql
if hash mysql 2>/dev/null; then
  echo "Testing mysql..."
  bash test/test_mysql.sh $1
fi


