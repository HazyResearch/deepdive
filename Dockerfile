FROM ubuntu
MAINTAINER Adam Goldberg <adamwgoldberg@gmail.com>
RUN sudo apt-get update
RUN sudo apt-get -y install emacs
RUN sudo apt-get -y install default-jre default-jdk
RUN sudo apt-get -y install python
RUN sudo apt-get -y install gnuplot
RUN sudo apt-get -y install postgresql postgresql-contrib
RUN sudo apt-get -y install git
RUN sudo apt-get -y install build-essential
RUN cd ~/ && git clone https://github.com/HazyResearch/deepdive.git
RUN sudo apt-get install zip unzip
RUN cd ~/deepdive && make

# Configure environment variables
RUN echo 'export PGUSER=postgres' >> ~/.bashrc
RUN echo 'export PGPORT=$POSTGRES_PORT_5432_TCP_PORT' >> ~/.bashrc
RUN echo 'export PGHOST=$POSTGRES_PORT_5432_TCP_ADDR' >> ~/.bashrc
RUN echo 'export PGPASSWORD=password' >> ~/.bashrc
RUN echo 'export PGUSER=postgres' >> ~/.bashrc
RUN echo 'export DEEPDIVE_HOME=~/deepdive' >> ~/.bashrc
RUN echo 'export LD_LIBRARY_PATH=$DEEPDIVE_HOME/lib/dw_linux/lib:$DEEPDIVE_HOME/lib/dw_linux/lib64' >> ~/.bashrc
RUN echo 'export PATH=~/deepdive/sbt:$PATH' >> ~/.bashrc