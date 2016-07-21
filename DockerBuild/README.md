# DeepDive builds inside Docker containers

Here are several commands for developers to quickly run their builds/tests in [Docker](https://docker.io) containers.

There is a `Dockerfile` at the top of the source tree that can create a DeepDive build from scratch.
However, it is not suitable for development because it wastes too much time on repeating all steps that rarely change to ensure build/runtime dependencies, etc.
The recommended development workflow using Docker is to use an already published "master" Docker image that holds a relatively recent build.
After applying the source code changes to the image, an incremental build on top of it can be done much more quickly.
([`netj/deepdive-build:master`](https://hub.docker.com/r/netj/deepdive-build/) is DeepDive's master image.)

Scripts here support the typical build and test tasks using the master image as well as creating and updating it.

## Build/Test

### `build-in-container`
Builds any changes made from the host inside a container using the `latest` image.
A different image can be specified in `DockerBuild.conf`, `DOCKER_IMAGE` environment variable, or a tag as the first argument to the command.

### `test-in-container`
Runs tests against the latest build inside a container.
It commits the container as a image tagged either `latest-test-PASS` or `latest-test-FAIL` with and without the git branch as a prefix for the tag.

There are a few more scripts that launch other containers such as databases and run tests in containers linked to them.

* `test-in-container-postgres`
* `test-in-container-greenplum`

After a successful test, the `latest` image for the current git branch is automatically updated to the build which was used for running the tests.

### `inspect-container`
Starts an interactive shell or runs given command for inspecting the container holding the latest build or test results.
A tag for the container image (also overridable through `DOCKER_IMAGE` environment) to inspect can be specified as the first command line argument.

## Update Images

### `update-latest-image`
Makes the most recent build the new `latest` image, so subsequent `build-in-container` on different branches can start builds on top of it.
This script does not push the image to DockerHub, and should be done manually if desired:

```bash
# after updating master image locally
update-latest-image

# also publish it
docker push netj/deepdive-build:master
```

### `rebuild-latest-image-from-scratch`
Uses the `Dockerfile` at the top of the source tree to rebuild the master image and pushes to DockerHub.


## Clean up

Scripts here leaves each build and test in a new container image layer, which are kept as small as possible, but still can add up to a huge amount of wasted space.
Here are some tricks to clean them up.

```bash
# to kill and remove all containers
docker ps -qa | xargs docker rm -f

# to remove all images
docker images -qa | xargs docker rmi -f
```

### Garbage collect images
After many builds and tests, many Docker images will show up as having `<none>` REPOSITORY or TAG.
Here's a trick to clean those garbages:
```bash
docker images -q --filter 'dangling=true' | xargs docker rmi -f
```
