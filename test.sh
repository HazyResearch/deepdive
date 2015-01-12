set -e

# if the command psql exists, then test PostgreSQL
if hash psql 2>/dev/null; then
	echo "Testing psql..."
	bash test/test_psql.sh
fi

# if Greenplum is supported (gpfdist exist), then test Greenplum
# Since greenplum also has a 'psql' command, the condition in the previous test
# will also be satisfied. Tests are effectively run twice on GP: one using only
# the PostgreSQL features and one using the GreenPlum features (e.g., parallel
# grounding, gpfdist)
if hash gpfdist 2>/dev/null; then
	echo "Testing Greenplum..."
	bash test/test_gp.sh
fi

# if the command mysql exists, then test mysql
if hash mysql 2>/dev/null; then
	echo "Testing mysql..."
	bash test/test_mysql.sh
fi

