#!/usr/bin/env bash
# DeepDive installers for Mac OS X

needs_brew() {
    has brew || error "This requires Homebrew (http://brew.sh)"
}

install__deepdive_build_deps() {
    needs_brew
    set -x
    javac -version
    xcode-select --install || true
    has git     || brew install git
    has unzip   || brew install unzip
    has xz      || brew install xz
}

install__deepdive_runtime_deps() {
    needs_brew
    set -x
    java -version
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
