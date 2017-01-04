#!/usr/bin/env bash
set -euo pipefail

test -e lib/gtest/libgtest.a
test -e lib/gtest-1.7.0/include/gtest/gtest.h

test -d lib/tclap/include/tclap

test -d lib/numactl/include

test -d lib/zeromq/include
test -e lib/zmq/zmq.hpp

test -d lib/msgpack/include
