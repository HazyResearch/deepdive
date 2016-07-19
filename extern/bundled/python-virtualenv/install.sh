#!/usr/bin/env bash
set -euo pipefail -x

unset PYTHONPATH  # existing PYTHONPATH can interfere

# install a Python virtualenv locally along with pip
[[ -e bootstrap/virtualenv.py ]] || {
    fetch-verify get-pip.py https://bootstrap.pypa.io/get-pip.py
    "$(type -p python2)" get-pip.py virtualenv --ignore-installed --target bootstrap
}
export PYTHONPATH="$PWD/bootstrap"
virtualenv() {
    # use the virtualenv locally installed
    # XXX --target doesn't give us the handy script while --prefix does, but
    # since the script oddly doesn't pick up the locally installed pip on
    # Travis CI, we basically need to explicitly force it here..
    # See: https://github.com/pypa/pip/pull/3723
    "$(type -p python2)" -c 'import virtualenv; virtualenv.main()' "$@"
}
virtualenv --always-copy prefix || virtualenv prefix
virtualenv --relocatable prefix

# install all requirements via pip
PATH="$PWD/../postgresql/prefix/bin:$PATH"  # psycopg2 requires postgresql's pg_config
PATH="$PWD/prefix/bin:$PATH"  # this ensures pip installs to virtualenv not to /usr
prefix/bin/pip2 install -r requirements.txt

# make sure things are properly exposed
shopt -s extglob
symlink-under-depends-prefix lib -d prefix/lib/python*
symlink-under-depends-prefix bin -x prefix/bin/!(activate@(|.*|_this.py)|python@(|[23]*|-config)|pip@(|[23]*)|easy_install@(|-[23]*)|wheel)
