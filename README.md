# DeepDive

Licensed under the Apache License, Version 2.0. http://www.apache.org/licenses/LICENSE-2.0.txt

Tested with Travis CI. 
[![Build Status](https://travis-ci.org/HazyResearch/deepdive.svg?branch=master)](https://travis-ci.org/HazyResearch/deepdive)

### [Visit The DeepDive Website](http://deepdive.stanford.edu)

Docker instructions:
<pre>
# Make directory for greenplum data persistence
mkdir -p /opt/greenplum/data 

# Build and tag deepdive image 
docker pull adamwgoldberg/deepdive

# Pull my greenplum image from Docker Hub. Contact me if you need access to the private repository on Docker Hub. 
# Omit the -v flag portion ("-v /opt/greenplum/data:/app") if you do not desire to save your data.
docker run -d -v /opt/greenplum/data:/app --privileged --name db -h gphost adamwgoldberg/greenplum

# Make directory for deepdive code persistence
mkdir -p /opt/deepdive/app

# Run Deepdive
# As above, omit the -v flag portion if you do not desire to save your application code.
# All deepdive application code should be created in /root/deepdive/app
docker run -t -d -v /opt/deepdive/app:/root/deepdive/app --link db:db --name deepdive adamwgoldberg/deepdive bash

# Attach shell to Deepdive 
docker exec -ti deepdive bash

# Inside of that shell run:
cd ~/deepdive
make test
</pre>

Docker tips:
* AWS EC2 m.xlarge on Virginia region using ami-84e897ec is a great place to start
* Ensure you have at least 20GB of storage 
* Any machine with Docker installed should work fine

