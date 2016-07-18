## common configuration for DockerBuild scripts

# call other scripts by name
Here=$(cd "$(dirname "$BASH_SOURCE")" && pwd)
PATH="$Here/util:$Here:$PATH"

# load configuration in DockerBuild.conf
! [[ -r "$Here".conf ]] || . "$Here".conf

# default values
: ${DOCKER_IMAGE_MASTER:=$USER/build:master}
: ${DOCKER_IMAGE:=${DOCKER_IMAGE_MASTER%%:*}}
: ${DOCKER_IMAGE_TEST_PREFIX:=${DOCKER_IMAGE_MASTER%%:*}:test.}
: ${DOCKER_HOST_MOUNTPOINT:=/mnt}
: ${DOCKER_HOST_PATH:=$(dirname "$Here")}
: ${DOCKER_CONTAINER:=$(basename "$DOCKER_HOST_PATH")-build}
: ${DOCKER_RUN_OPTS:=}

# default build and test commands (should be quoted)
: ${DOCKER_BUILD_COMMAND:='make -j'}
: ${DOCKER_TEST_COMMAND:='make -j test'}
