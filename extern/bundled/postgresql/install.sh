#!/usr/bin/env bash
# install PostgreSQL
set -eu

: ${POSTGRESQL_DOWNLOAD_BASEURL:=https://ftp.postgresql.org/pub/source}
: ${POSTGRESQL_VERSION:=9.5.3}

name=postgresql
version=$POSTGRESQL_VERSION
ext=.tar.bz2
url=$POSTGRESQL_DOWNLOAD_BASEURL/v$version/$name-$version$ext

fetch-configure-build-install $name-$version <<END
url=$url
sha256sum=$url.sha256
md5sum=$url.md5
END
