# Dockerfile for building an image for running DeepDive build and tests inside containers
FROM ubuntu
MAINTAINER deepdive-dev@googlegroups.com

# Install essential stuffs
RUN apt-get update && apt-get install -qy \
        sudo \
        coreutils \
        bash \
        curl \
        git \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

ENV USER=root
WORKDIR /deepdive-build-base

# Install deepdive build/runtime dependencies
ARG BRANCH=master
ARG RELEASE=UNSTABLE
ENV BRANCH=$BRANCH
ENV RELEASE=$RELEASE
ENV INSTALLER_LOCAL_FIRST=true
COPY install    install
COPY install.sh install.sh
RUN ./install.sh \
        _deepdive_build_deps \
        _deepdive_runtime_deps \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Get a fresh clone of deepdive
RUN GITCLONE=/deepdive ./install.sh _deepdive_git_repo

# Build deepdive
WORKDIR /deepdive
RUN FORCE_UNSAFE_CONFIGURE=1 \
    make bundled-runtime-dependencies
RUN make
