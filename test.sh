set -e

# if command psql exist, then test psql
if hash psql 2>/dev/null; then
  # Set postgres username and password
  export PGUSER=${PGUSER:-`whoami`}
  export PGPASSWORD=${PGPASSWORD:-}
  export PGPORT=${PGPORT:-5432}
  export PGHOST=${PGHOST:-localhost}
  export DBNAME=deepdive_test
  echo "Testing psql..."
  bash test/test_psql.sh
fi

# if command mysql exist, then test mysql
if hash mysql 2>/dev/null; then
  # Set mysql username and password
  export DBUSER=${DBUSER:-root}
  export DBPASSWORD=${DBPASSWORD:-}
  export DBHOST=${DBHOST:-127.0.0.1}
  export DBPORT=${DBPORT:-3306}
  export DBNAME=deepdive_test
  echo "Testing mysql..."
  bash test/test_mysql.sh
fi

# if greenplum supported (gpfdist exist), then test GP
if hash gpfdist 2>/dev/null; then
  # Set mysql username and password
  export PGUSER=${PGUSER:-`whoami`}
  export PGPASSWORD=${PGPASSWORD:-}
  export PGPORT=${PGPORT:-5432}
  export PGHOST=${PGHOST:-localhost}
  export DBNAME=deepdive_test
  echo "Testing Greenplum..."
  bash test/test_gp.sh
fi

echo "Testing Other modules..."
bash src/test/python/test_ddext/test.sh

