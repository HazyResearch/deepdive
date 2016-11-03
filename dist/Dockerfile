# Dockerfile for DeepDive users
FROM jupyter/minimal-notebook
MAINTAINER deepdive-dev@googlegroups.com

# install DeepDive and its runtime dependencies
ADD deepdive-build.tar.gz /deepdive
ADD install.sh /deepdive/
ADD install    /deepdive/install
USER root
RUN for cmd in /deepdive/bin/*; do ln -sfn "$cmd" /usr/local/bin/; done
RUN INSTALLER_LOCAL_FIRST=true /deepdive/install.sh _deepdive_runtime_deps
RUN apt-get install -qy postgresql-client
USER jovyan

# override default behavior
CMD exec start-notebook.sh --ip 0.0.0.0
