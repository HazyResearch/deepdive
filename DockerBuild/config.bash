## common configuration for DockerBuild scripts

# load configuration in DockerBuild.conf
! [[ -r "${BASH_SOURCE%/config.bash}".conf ]] || . "${BASH_SOURCE%/config.bash}".conf

# default values
Here=$(cd "$(dirname "$BASH_SOURCE")" && pwd)
: ${DOCKER_IMAGE_MASTER:=$USER/build:master}
: ${DOCKER_IMAGE:=${DOCKER_IMAGE_MASTER%%:*}}
: ${DOCKER_IMAGE_TEST_PREFIX:=${DOCKER_IMAGE_MASTER%%:*}:test.}
: ${DOCKER_HOST_MOUNTPOINT:=/mnt}
: ${DOCKER_HOST_PATH:=$(dirname "$Here")}
: ${DOCKER_CONTAINER:=$(basename "$DOCKER_HOST_PATH")-build}
: ${DOCKER_RUN_OPTS:=}

# call other scripts by name
PATH="$Here:$PATH"
