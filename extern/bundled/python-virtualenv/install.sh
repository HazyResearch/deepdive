#!/usr/bin/env bash
set -euo pipefail -x

unset PYTHONPATH  # existing PYTHONPATH can interfere

( # install a Python virtualenv
if ! type virtualenv &>/dev/null; then
    # if virtualenv isn't available, install it locally along with pip
    [[ -e bootstrap/virtualenv.py ]] || {
        curl -RLO https://bootstrap.pypa.io/get-pip.py &&
        python get-pip.py virtualenv --ignore-installed --target bootstrap
    }
    virtualenv() {
        # use the virtualenv locally installed
        # XXX --target doesn't give us the handy script while --prefix does, but
        # since the script oddly doesn't pick up the locally installed pip on
        # Travis CI, we basically need to explicitly force it here..
        # See: https://github.com/pypa/pip/pull/3723
        PYTHONPATH="$PWD/bootstrap" \
            python -c 'import virtualenv; virtualenv.main()' "$@"
    }
fi
virtualenv --always-copy prefix || virtualenv prefix
virtualenv --relocatable prefix
)

# turn these off and on here since Python virtualenv scripts are too brittle to run in strict mode
set +euo pipefail
source prefix/bin/activate
set -euo pipefail

# install all requirements via pip
pip install -r requirements.txt

# make sure things are properly exposed
shopt -s extglob
symlink-under-depends-prefix lib -d prefix/lib/python*
symlink-under-depends-prefix bin -x prefix/bin/!(activate@(|.*|_this.py)|python@(|[23]*|-config)|pip@(|[23]*)|easy_install@(|-[23]*)|wheel)
