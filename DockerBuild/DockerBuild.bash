## common configuration for DockerBuild scripts

# call other scripts by name
Here=$(cd "$(dirname "$BASH_SOURCE")" && pwd)
PATH="$Here/util:$Here:$PATH"

# load configuration in DockerBuild.conf
! [[ -r "$Here".conf ]] || . "$Here".conf

# default values
: ${DOCKER_HOST_MOUNTPOINT:="/mnt"}
: ${DOCKER_HOST_PATH:="$(dirname "$Here")"}
: ${DOCKER_CONTAINER:="$(basename "$DOCKER_HOST_PATH")-build"}
: ${DOCKER_IMAGE:="$USER/$DOCKER_CONTAINER"}
: ${DOCKER_IMAGE_LATEST_STABLE:="$DOCKER_IMAGE:latest"}
: ${DOCKER_IMAGE_LATEST_BUILD:="$DOCKER_IMAGE:latest-build"}
: ${DOCKER_IMAGE_LATEST_TEST:="$DOCKER_IMAGE:latest-test"}
: ${DOCKER_IMAGE_BRANCH_FORMAT_STABLE:="$DOCKER_IMAGE:%s.latest"}
: ${DOCKER_IMAGE_BRANCH_FORMAT_BUILD:="$DOCKER_IMAGE:%s.latest-build"}
: ${DOCKER_IMAGE_BRANCH_FORMAT_TEST:="$DOCKER_IMAGE:%s.latest-test"}
: ${DOCKER_IMAGE_BRANCH_FORMAT_RUN:="$DOCKER_IMAGE:%s.latest-run"}
: ${DOCKER_RUN_OPTS:=}

# default build and test commands (should be quoted)
: ${DOCKER_BUILD_COMMAND:='make -j'}
: ${DOCKER_TEST_COMMAND:='make -j test'}
