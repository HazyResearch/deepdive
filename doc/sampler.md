---
layout: default
---

# Changing Sampler

This tutorial explains how to use DimmWitted, a faster sampler for DeepDive.

### Downloading DimmWitted

Download the sampler from github using the following commands

    >> cd [desired directory for the sampler]
    >> git clone https://github.com/zhangce/dimmwitted.git

This will create a directory named dimmwitted. The release subdirectory includes pre-built binaries in the for both Linux and Mac. Now go into that directory and decompress the file

	>> tar xf dw.tar.bz2

If you are on a Mac, run instead

	>> tar xf dw_mac.tar.bz2

You will get a binary file `dw`. You can check if the sampler works by running the test

	>> sh test.sh

### Using DimmWitted

To use the sampler, you need to specify the sampler path to the binary in application.conf file set, like

	sampler.sampler_cmd: "[sampler path to binary]/dw gibbs

Also, you may want to change the sampler arguments to get better [performance tuning](performance.html)

	sampler.sampler_args: "-l 1000 -s 1 -i 1000 --alpha 0.01"