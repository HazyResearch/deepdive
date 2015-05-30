#!/bin/bash


cd ~
git clone https://github.com/hazyresearch/deepdive.git
cd deepdive
make

cd ~
git clone https://github.com/hazyresearch/sampler.git
cd sampler
make

git clone https://github.com/hazyresearch/mindbender.git
cd mindbender
make
# Note: requires user input


