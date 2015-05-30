#!/usr/bin/env bash
# DeepDive installers for Mac OS X

list_installers() {
    list_common_installers
    echo install_Postgres
}

install_runtime_deps() {
    if has brew; then
        set -x
        javac -version
        xcode-select --install || true
        has unzip   || brew install unzip
        has gnuplot || brew install gnuplot
        has git     || brew install git
    else
        error "Cannot install dependencies without Homebrew (http://brew.sh)"
    fi
}

install_Postgres() {
    has psql && brew info postgres || brew install postgres
}
