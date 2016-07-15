## common configuration for DeepDive's Docker build scripts

# default values
Here=$(cd "$(dirname "$BASH_SOURCE")" && pwd)
: ${DEEPDIVE_SOURCE_ROOT:=$(cd "$Here/../../.." && pwd)}
: ${DOCKER_CONTAINER:=deepdive-build}
: ${DOCKER_IMAGE_MASTER:=netj/deepdive-build:master}
: ${DOCKER_IMAGE:=${DOCKER_IMAGE_MASTER%%:*}}
: ${DOCKER_IMAGE_TEST_PREFIX:=${DOCKER_IMAGE_MASTER%%:*}:test.}
: ${DOCKER_HOST_PATH:=$DEEPDIVE_SOURCE_ROOT}
: ${DOCKER_HOST_MOUNTPOINT:=/mnt}

# use error, warning, etc.
PATH="$DEEPDIVE_SOURCE_ROOT/shell:$PATH"

# call other scripts by name
PATH="$Here:$PATH"
