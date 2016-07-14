#!/usr/bin/env bash
# DeepDive installers for Mac OS X

needs_brew() {
    has brew || error "This requires Homebrew (http://brew.sh)"
}

install__deepdive_build_deps() {
    needs_brew
    set -x
    # check if java -version >= 1.8
    javaVersion=$(javac -version 2>&1 | sed -e '1!d; s/^javac //')
    [[ ! $javaVersion < 1.8 ]] || { echo >&2 "javac -version >= 1.8 required but found: $javaVersion"; false; }
    xcode-select --install || true
    has make    || make --version
    has git     || brew install git
    # coreutils, etc., GNU
    has git     || brew install coreutils
    has xz      || brew install xz
    # sampler build dependencies
    has unzip   || brew install unzip
    has cmake   || brew install cmake
    # graphviz-devel
    has autoconf || brew install autoconf
    has pkg-config || brew install pkg-config
}

install__deepdive_runtime_deps() {
    needs_brew
    set -x
    # check if java -version >= 1.8
    javaVersion=$(java -version 2>&1 | sed -e '1!d; s/^java version "//; s/"$//')
    [[ ! $javaVersion < 1.8 ]] || { echo >&2 "java -version >= 1.8 required but found: $javaVersion"; false; }
    make --version
    has gnuplot || brew install gnuplot
}

install_postgres() {
    needs_brew
    if has psql
    then
        brew info postgres | grep 'postgres -D ' | bash &
    else
        brew uninstall postgres || true
        brew install postgres
        brew info postgres | grep 'postgres -D ' | bash &
    fi
}
