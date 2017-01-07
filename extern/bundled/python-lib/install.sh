#!/usr/bin/env bash
set -euo pipefail -x

unset PYTHONPATH  # existing PYTHONPATH can interfere

# install Python requirements with pip
fetch-verify get-pip.py https://bootstrap.pypa.io/get-pip.py
for python in python2.7 python3.6 python3.5; do
    type $python || continue
    PYTHONPATH="$PWD"/prefix/lib/$python/site-packages \
    $python get-pip.py setuptools -r requirements.txt --upgrade --prefix prefix
done

# remove pip and setuptools
shopt -s extglob
rm -rf prefix/bin/@(pip@(|[23]*)|wheel) prefix/lib/python*/site-packages/@(pip|setuptools)@(|-*)

# make sure no entrypoints have absolute path to python in its shebang
for cmd in prefix/bin/*; do
    head -1 "$cmd" | grep -q '^#!/[^[:space:]]*/python.*$' || continue
    sed -e '1s:^#!/[^[:space:]]*/\(python.*\)$:#!/usr/bin/env \1:' -i~ "$cmd"
    rm -f "$cmd"~
done

# make sure things are properly exposed
symlink-under-depends-prefix lib -d prefix/lib/python*
symlink-under-depends-prefix bin -x prefix/bin/!(activate@(|.*|_this.py)|python@(|[23]*|-config)|pip@(|[23]*)|easy_install@(|-[23]*)|wheel)
