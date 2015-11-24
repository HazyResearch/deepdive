---
layout: default
title: Fusion Installation Guide
---

# Fusion Installation Guide

This document explains how to install Fusion on your system.
Fusion currently only runs on Linux.

Dependencies

* libsodium 1.0.5 (required by zeromq)
* zeromq 4.1.3

Installation steps.

1. Install DeepDive from `fusion` branch of DeepDive repo.
The branch can be cloned using the following command.

    ```bash
    git clone https://github.com/HazyResearch/deepdive.git -b fusion
    ```

    Please follow the rest of DeepDive installation guide to finish the installation of DeepDive.

2. Install Caffe from `fusion` branch of `https://github.com/feiranwang/caffe`.
The branch can be cloned using the following command.

    ```bash
    git clone https://github.com/feiranwang/caffe.git -b fusion
    ```

Note if you install zeromq and libsodium in a non-standard location, you need to add corresponding libsodium and zeromq folders to `INCLUDE_DIRS` and `LIBRARY_DIRS` in `Makefile.config`.
For example, if zeromq is install in `zeromq_folder`, then add `zeromq_folder/include` to `INCLUDE_DIRS`, and `zeromq_folder/lib` to `LIBRARY_DIRS`.

After Caffe installation, build Caffe.
Then, add caffe to environment variable `PATH` using
```
export PATH=/path/to/caffe/build/tools/:$PATH
```

Also, add caffe lib to environment variable `LD_LIBRARY_PATH` using
```
export LD_LIBRARY_PATH=/path/to/caffe/build/lib/:$LD_LIBRARY_PATH
```
