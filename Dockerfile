# Dockerfile to build and test DeepDive inside a container
FROM deepdive-build-base
MAINTAINER deepdive-dev@googlegroups.com

WORKDIR /deepdive

# Install any updated build/runtime dependencies
COPY util/install    /deepdive/util/install
COPY util/install.sh /deepdive/util/install.sh
RUN INSTALLER_LOCAL_FIRST=true \
    util/install.sh \
        _deepdive_build_deps \
        _deepdive_runtime_deps \
 && sudo apt-get clean \
 && sudo rm -rf /var/lib/apt/lists/*

# Copy the rest of the source tree
COPY . /deepdive/

# Build the copied source
RUN make
