# Dockerfile for building an image for running DeepDive build and tests inside containers
FROM ubuntu
MAINTAINER deepdive-dev@googlegroups.com

# Install essential stuffs
RUN apt-get update && apt-get install -qy \
        coreutils \
        bash \
        curl \
        git \
        sudo \
 && apt-get clean \
 && rm -rf /var/lib/apt/lists/*

# Set up a non-superuser
ARG USER=user
ENV USER=$USER
RUN adduser --disabled-password --gecos "" $USER \
 && adduser $USER adm \
 && bash -c "echo '%adm ALL=(ALL:ALL) NOPASSWD: ALL' | tee -a /etc/sudoers"
USER $USER

WORKDIR /deepdive-build-base

# Install deepdive build/runtime dependencies
ARG BRANCH=master
ARG RELEASE=UNSTABLE
ENV BRANCH=$BRANCH
ENV RELEASE=$RELEASE
ENV INSTALLER_LOCAL_FIRST=true
COPY install    install
COPY install.sh install.sh
RUN sudo chown -R $USER .
RUN ./install.sh \
        _deepdive_build_deps \
        _deepdive_runtime_deps \
 && sudo apt-get clean \
 && sudo rm -rf /var/lib/apt/lists/*

# Get a fresh clone of deepdive
WORKDIR /deepdive
RUN sudo chown -R $USER .
RUN GITCLONE=$PWD \
    /deepdive-build-base/install.sh _deepdive_git_repo

# Build deepdive
RUN make bundled-runtime-dependencies
RUN make
