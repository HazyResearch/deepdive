#!/usr/bin/env bash
set -euo pipefail

# ensure other local::lib doesn't interfere
eval $(perl -Mlocal::lib=--deactivate-all 2>/dev/null || true)
unset PERL5LIB PERL_LOCAL_LIB_ROOT PERL_MB_OPT PERL_MM_OPT

# install cpanm with a local::lib to bundle
perl-local-lib() { perl -Iprefix/lib/perl5 -Mlocal::lib="$PWD"/prefix; }
perl-local-lib &>/dev/null ||
    # See: http://stackoverflow.com/a/2980715/390044
    curl -fsSL http://cpanmin.us | perl - -l prefix App::cpanminus local::lib
set +u  # perl can dereference PERL_LOCAL_LIB_ROOT and other unset variables
eval $(perl-local-lib)
set -u

# install all CPAN modules to bundle
cpanm JSON JSON::XS
cpanm Bundle::LWP

# make sure things are properly exposed
symlink-under-depends-prefix bin -x prefix/bin/*
symlink-under-depends-prefix lib -d prefix/lib/perl5
