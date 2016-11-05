# Dockerfile for DeepDive users
FROM jupyter/minimal-notebook
MAINTAINER deepdive-dev@googlegroups.com

# add DeepDive installer to the image
ADD install.sh /deepdive/
ADD install    /deepdive/install

# switch to root to install a few things
USER root

# install DeepDive and its runtime dependencies
RUN INSTALLER_LOCAL_FIRST=true /deepdive/install.sh _deepdive_runtime_deps

# install postgres client
RUN apt-get install -qy curl wget ca-certificates \
 && apt-add-repository -y "deb http://apt.postgresql.org/pub/repos/apt/ $(lsb_release -cs)-pgdg main" \
 && wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | sudo apt-key add - \
 && apt-get update -qy \
 && apt-get upgrade -qy \
 && apt-get install -qy postgresql-client-9.5 \
 ;

# NOTE that we're trying to keep as much as possible under the user so they can be freely modified
USER jovyan
ENV USER=jovyan

# add DeepDive itself
ADD deepdive-build.tar.gz /deepdive
USER root
RUN for cmd in /deepdive/bin/*; do ln -sfn "$cmd" /usr/local/bin/; done
USER jovyan

# preinstall CoreNLP
ADD stanford-corenlp /deepdive/lib/stanford-corenlp
USER root
RUN chown -R jovyan /deepdive/lib/stanford-corenlp
USER jovyan
RUN deepdive corenlp install
RUN rm -f /deepdive/lib/stanford-corenlp/stanford-corenlp-*.zip

# shorten working dir
WORKDIR /ConfinedWater

# include examples
ADD deepdive-examples.tar.gz deepdive-examples

# override default behavior
CMD exec start-notebook.sh --ip 0.0.0.0
