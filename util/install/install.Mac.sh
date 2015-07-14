#!/usr/bin/env bash
# DeepDive installers for Mac OS X

install__deepdive_build_deps() {
    has brew || error "Cannot install dependencies without Homebrew (http://brew.sh)"
    set -x
    javac -version
    xcode-select --install || true
    has git     || brew install git
    has unzip   || brew install unzip
}

install__deepdive_runtime_deps() {
    has brew || error "Cannot install dependencies without Homebrew (http://brew.sh)"
    set -x
    java -version
    has gnuplot || brew install gnuplot
}

install_postgres() {
    if has psql; then
        brew info postgres | grep 'postgres -D ' | bash &
    else
        brew install postgres
    fi
}
