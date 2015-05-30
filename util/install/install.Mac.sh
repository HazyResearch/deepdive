#!/usr/bin/env bash
# DeepDive installers for Mac OS X

install_runtime_deps() {
    if has brew; then
        set -x
        brew install unzip gnuplot
    elif has port; then
        set -x
        sudo port install unzip gnuplot
    else
        error "Cannot install dependencies without brew or MacPorts"
    fi
}
