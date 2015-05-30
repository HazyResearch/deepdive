#!/bin/bash

# Installation of the dependencies for the DeepDive stack.
# (deepdive, sampler, mindbender)

sudo apt-get update

# automate license agreement for oracle java
echo debconf shared/accepted-oracle-license-v1-1 select true | \
  sudo debconf-set-selections
echo debconf shared/accepted-oracle-license-v1-1 seen true | \
  sudo debconf-set-selections

# install oracle java
sudo apt-get install -y python-software-properties &&
sudo add-apt-repository -y ppa:webupd8team/java &&
sudo apt-get update &&
sudo apt-get install -y oracle-java8-installer 

# install additional packages for deepdive
sudo apt-get install -y make gnuplot awscli unzip 

# install additional packages for sampler
sudo apt-get install -y g++ cmake libnuma-dev libtclap-dev

# install additional packages for mindbender
sudo apt-get install -y libyaml-dev python-pip
sudo apt-get install -y unzip

# install performance analysis tools
sudo apt-get install -y sysstat

