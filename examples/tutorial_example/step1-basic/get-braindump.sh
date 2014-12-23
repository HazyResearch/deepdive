#!/usr/bin/env bash
# A script to start Mindtagger for tasks under labeling/
# Author: Jaeho Shin <netj@cs.stanford.edu>
# Created: 2014-10-11
set -eu

# set up environment to run Mindtagger
cd "$(dirname "$0")"
PATH="$PWD:$PATH"

# install BrainDump locally if not available or broken
VERSION=0.1.1
if ! type braindump &>/dev/null; then
    tool=braindump
    mkdir -p "$(dirname "$tool")"
    echo >&2 "Downloading BrainDump..."
    curl -k --location --show-error --output $tool.download.zip \
    		https://github.com/zifeishan/braindump/archive/v${VERSION}.zip
    unzip $tool.download.zip
    mkdir -p $HOME/local
    rm -rf $HOME/local/braindump-src
    rm -f $tool.download.zip
    mv -f $tool-${VERSION} $HOME/local/braindump-src 
    cd $HOME/local/braindump-src 
    make
else
    echo "BrainDump is already installed in `which braindump`. Remove it if you want to re-install."
fi
