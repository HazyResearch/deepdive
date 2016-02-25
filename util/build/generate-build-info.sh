#!/usr/bin/env bash
# generate-build-info.sh -- A script for generating build information
# Author: Jaeho Shin <netj@cs.stanford.edu>
# Created: 2015-07-12
set -eu

# build information to print
build_info=(
    build_timestamp="$(date +%FT%T%z | sed 's/\(.*\)\([0-9][0-9]\)/\1:\2/')"
    build_os="$(uname)"
    build_os_release="$(uname -r)"
    build_os_version="$(uname -v)"
    build_machine="$(uname -m)"
    build_hostname="$(hostname -f || true)"
    version
    version_long
    version_commit
)
# TODO record Mac, Linux distro specific info

# determine version from git or use current date and time
if git rev-parse HEAD &>/dev/null; then
    version=$(git describe || true)
    version_long=$(git describe --long || true)
    version_commit=$(git rev-parse HEAD)

    # work-in-progress? (is the working tree dirty?)
    [[ $(git status --porcelain | wc -l) -eq 0 ]] || version_commit+="+WIP"
else
    version_long=$(date +0.0.%Y%m%d-%H%M%S)
    version=${version_long%-*}
    version_commit=
fi

# print build info in POSIX shell syntax
print() {
    local name= value=
    for name; do
        case $name in
            *=*)  # support name=value args
                value=${name#*=}
                name=${name%%=*} ;;
            *)    # use $name as value for name args
                value=${!name}
        esac
        local valueSafe=$value
        [[ -z "${valueSafe//[a-zA-Z0-9._-]/}" ]] ||
            # escape unsafe values with single quotes
            valueSafe="'${valueSafe//\'/\'\\\'\'}'"
        printf '%s=%s\n' "$name" "$valueSafe"
    done
}
print "${build_info[@]}"
