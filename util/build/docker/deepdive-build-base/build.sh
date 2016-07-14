#!/usr/bin/env bash
# deepdive-build-base/build.sh -- Rebuilds and publishes deepdive-build-base Docker image
##
set -euo pipefail

: ${DOCKER_IMAGE:=netj/deepdive-build-base}
type docker
docker -v

set -x
cd "$(dirname "$0")"

# copy files into build context
chmod +w install{.sh,/*} || true
cp -av ../../../install* .
chmod a-w install{.sh,/*}

# build Docker image
docker build -t "$DOCKER_IMAGE" .

# and publish
docker push "$DOCKER_IMAGE"
