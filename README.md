# DimmWitted: Fast Gibbs Sampler [![Build Status](https://travis-ci.org/HazyResearch/sampler.svg)](https://travis-ci.org/HazyResearch/sampler)

## How fast is DimmWitted?

  - On Amazon EC2's FREE MACHINE (512M memory, 1 core). We can sample 3.6M variables/seconds.
  - On a 2-node Amazon EC2 machine, sampling 7 billion random variables, each of which has 10 features, takes 3 minutes. This means we can run inference for all living human beings on this planet with $15 (100 samples!)
  - On Macbook, DimmWitted runs 10x faster than DeepDive's default sampler.

## Usage

See: [DimmWitted sampler page in DeepDive's documentation](http://deepdive.stanford.edu/sampler).

The binary format for DimmWitted's input is documented in [doc/binary_format.md](https://github.com/HazyResearch/sampler/blob/master/doc/binary_format.md).

## Installation

First, install build dependencies:

    make -j dep

Then, build:

    make -j

A modern C++ compiler is required: g++ >= 4.8 or clang++ >= 4.2.
To specify the compiler to use, set the `CXX` variable:

    CXX=/dfs/rulk/0/czhang/software/gcc/bin/g++ make

To test, run:

    make -j test


## Development

* Follow [Google C++ Style Guide](https://google.github.io/styleguide/cppguide.html).
* Travis CI tests will error unless you run `make format` before git commits.
* Tests are written with [gtest](https://github.com/google/googletest) and [bats](https://github.com/sstephenson/bats).
* Command-line parsing is done with [TCLAP](http://tclap.sourceforge.net).
* NUMA control is done with [libnuma](http://oss.sgi.com/projects/libnuma/).


## Reference

[C. Zhang and C. Ré. DimmWitted: A study of main-memory statistical analytics. PVLDB, 2014.](http://www.vldb.org/pvldb/vol7/p1283-zhang.pdf)
