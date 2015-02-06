# DeepDive

Licensed under the Apache License, Version 2.0. http://www.apache.org/licenses/LICENSE-2.0.txt

Tested with Travis CI.
[![Build Status](https://travis-ci.org/HazyResearch/deepdive.svg?branch=master)](https://travis-ci.org/HazyResearch/deepdive)

### [Visit The DeepDive Website](http://deepdive.stanford.edu)

Docker instructions:
<pre>
# Build and tag deepdive image
# You may specify 'latest' or 'dev' as the tag
docker pull adamwgoldberg/deepdive-github:develop

# Pull my greenplum image from Docker Hub. Contact me if you need access to the private repository on Docker Hub.
docker run -d --privileged --name db -h gphost adamwgoldberg/greenplum

# Run Deepdive
# All deepdive application code should be created in /root/deepdive/app
# Make sure the deepdive-github tag matches the above one.
docker run -t -d --link db:db --name deepdive adamwgoldberg/deepdive-github:develop bash

# Attach shell to Deepdive
# You may need to wait several minutes for Greenplum to initialize.
# The bash shell will say "Waiting for DB..." until it finishes.
docker exec -ti deepdive bash

# Inside of that shell run:
cd ~/deepdive
make test
</pre>

Docker tips:
* AWS EC2 m.xlarge on Virginia region using ami-84e897ec is a great place to start
* Ensure you have at least 20GB of storage
* Any machine with Docker installed should work fine
* Due to licensing, Greenplum is not freely available outside of our lab. You may wish to use a Dockerized postgres instead.
