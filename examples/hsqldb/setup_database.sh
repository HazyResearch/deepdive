#! /usr/bin/env bash

. "env.sh"

SQL_CLIENT_CMD="java -jar $DEEPDIVE_HOME/lib/sqltool.jar --autoCommit --inlineRc=url=jdbc:hsqldb:hsql://localhost/$DBNAME;user=SA,password="

$SQL_CLIENT_CMD setup_database.sql