#!/usr/bin/env bash
set -euo pipefail

unset PYTHONPATH  # existing PYTHONPATH can interfere

# install Python virtualenv
virtualenv prefix || exit $?
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
