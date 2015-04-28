DimmWitted
==========

# How fast is DimmWitted?

  - On Amazon EC2's FREE MACHINE (512M memory, 1 core). We can sample 3.6M varialbes/seconds.
  - On a 2-node Amazon EC2 machine, sampling 7 billion random variables, each of which has 10 features, takes 3 minutes. This means we can run inference for all living human beings on this planet with $15 (100 samples!)
  - On Macbook, DimmWitted runs 10x faster than DeepDive's default sampler.

# Usage

See: the [DimmWitted sampler page in DeepDive's documentation](http://deepdive.stanford.edu/doc/basics/sampler.html).

The binary format for DimmWitted's input is specified in [DeepDive's factor graph schema reference](http://deepdive.stanford.edu/doc/advanced/factor_graph_schema.html).

# Installation

First, install dependencies

    make dep

Then

    make

This will use whatever in you $(CXX) variable to compile. We assume that you have > g++4.7.2 or clang++-4.2. To specify a compiler to use, type in something like

    CXX=/dfs/rulk/0/czhang/software/gcc/bin/g++ make

On MacOS, `CXX=/opt/local/bin/clang++ make`

To test, run

    make test

On MacOS, you can use MacPorts to install clang.

    port select --list clang
    sudo port install clang-3.7
    sudo port select --set clang mp-clang-3.7 

You can then compile using

    CXX=/opt/local/bin/clang++ make

# Reference

[C. Zhang and C. Ré. DimmWitted: A study of main-memory statistical analytics. PVLDB, 2014.](http://www.vldb.org/pvldb/vol7/p1283-zhang.pdf)
