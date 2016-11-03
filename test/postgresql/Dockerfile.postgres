# Dockerfile for PostgreSQL 9.5 with PL/Python to test DeepDive
# See: https://hub.docker.com/r/hazyresearch/postgres/
#
# To rebuild and update the image, run:
# $ docker build -t hazyresearch/postgres - <Dockerfile.postgres
# $ docker push hazyresearch/postgres

FROM postgres:9.5
MAINTAINER deepdive-dev@googlegroups.com

RUN apt-get update \
 && apt-get install -y postgresql-server-dev-${PG_MAJOR} postgresql-plpython-${PG_MAJOR} \
 && apt-get clean \
 && rm -rf /var/lib/apt/lists/*

RUN echo 'PGPASSWORD="$POSTGRES_PASSWORD" psql -U $POSTGRES_USER template1 -c "CREATE EXTENSION plpythonu;"' >/docker-entrypoint-initdb.d/plpythonu.sh
