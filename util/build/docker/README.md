# DeepDive utilities for builds inside Docker containers

Here are some utilities to simplify the build and tests using Docker.

The recommended workflow is to run tests after an incremental build on top of an already published Docker image that holds a relatively recent build.
This is because it would waste too much time for the build to repeat every time all steps that rarely change taking care of the build/runtime dependencies.
The `Dockerfile` at the top of the source tree can build such master images from scratch.
A set of scripts here supports a handful of typical tasks using and updating the master image.

## host/ utilities

Several commands are available for developers to quickly start and manage the builds/tests in Docker containers:

* `create-container`
    Runs a new Docker container with the master image, mounting the host source tree inside the container.

* `build-in-container`
    Builds inside the container any changes made from the host.

* `test-in-container`
    Runs tests inside the container.

* `run-in-container`
    Runs arbitrary command inside the container.

* `inspect-container`
    Starts an interactive shell for inspecting the container.

* `remove-container`
    Kills and removes the Docker container.



### Snapshot and master images

* `update-master-image`
    Uses the `Dockerfile` to rebuild the master image and pushes to DockerHub.

    ```bash
    docker build -t netj/deepdive-build:master https://github.com/HazyResearch/deepdive/raw/master/Dockerfile
    ```

* `snapshot-container TAG`
    Commits the diff of the running container with given TAG and pushes to DockerHub.



## container/ utilities
Several commands are installed in the master image to support builds and tests.
Most of these are mainly used by the host commands, but they can be useful during `inspect-container` sessions.

* `import-changes-from-host`
* `export-changes-to-host`
* `build-changes-from-host`
* `test-changes-from-host`
