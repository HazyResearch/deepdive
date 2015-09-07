#!/usr/bin/env bash
# parse-url.sh -- Parses a URL and sets relevant.
# > . parse-url.sh URL
##

url=${1:?No URL is given to parse}

dbtype=${url%%://*}
url_without_scheme=${url#*://}  # strip URL scheme
rest=${url_without_scheme#*/}
user_password_host_port=${url_without_scheme%%/*}
host_port=${user_password_host_port#*@}
user_password=${user_password_host_port%$host_port}
user_password=${user_password%@}
host=${host_port%:*}
port=${host_port#$host}
port=${port#:}
user=${user_password%:*}
password=${user_password#$user}
password=${password#:}
dbname=${rest%\?*}
querystring=${rest#$dbname}
