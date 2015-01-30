# DeepDive

Licensed under the Apache License, Version 2.0. http://www.apache.org/licenses/LICENSE-2.0.txt

Tested with Travis CI. 
[![Build Status](https://travis-ci.org/HazyResearch/deepdive.svg?branch=master)](https://travis-ci.org/HazyResearch/deepdive)

### [Visit The DeepDive Website](http://deepdive.stanford.edu)

Docker instructions:
<pre>
docker build -t deepdive .
docker run -d --privileged --name db -h gphost readr/greenplum
docker run -t -d --link db:db --name deepdive deepdive bash
docker exec -ti deepdive bash
# Inside of that shell run:
cd ~/deepdive
make test
</pre>

Docker tips:
* AWS EC2 m.xlarge on Virginia region using ami-84e897ec is a great place to start
* Ensure you have at least 20GB of storage 
* Any machine with Docker installed should work fine

