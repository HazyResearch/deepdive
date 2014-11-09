set -e

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

