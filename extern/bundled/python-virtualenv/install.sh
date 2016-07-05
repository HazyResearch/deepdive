#!/usr/bin/env bash
set -euo pipefail -x

unset PYTHONPATH  # existing PYTHONPATH can interfere

( # install a Python virtualenv
if ! type virtualenv &>/dev/null; then
    # if virtualenv isn't available, install it locally with pip
    [[ -x bootstrap/bin/pip ]] || {
        curl -RLO https://bootstrap.pypa.io/get-pip.py &&
        python get-pip.py --ignore-installed --prefix bootstrap
    }
    # set up environment to use for bootstrapping
    for d in "$PWD"/bootstrap/lib/python*/site-packages; do
        [[ -d "$d" ]] || continue
        export PYTHONPATH="$d${PYTHONPATH:+:$PYTHONPATH}"
    done
    PATH="$PWD/bootstrap/bin:$PATH"
    # XXX probing Travis
    type -a pip
    which pip
    pip --version
    bootstrap/bin/pip --version
    # install virtualenv locally with the local pip
    [[ -x bootstrap/bin/virtualenv ]] ||
        bootstrap/bin/pip install virtualenv --ignore-installed --prefix bootstrap
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
symlink-under-depends-prefix bin -x prefix/bin/!(activate@(|.*)|python@(|[23]*|-config)|pip@(|[23]*)|easy_install@(|-[23]*)|wheel)
