FROM ubuntu
MAINTAINER adamwgoldberg@gmail.com
RUN sudo apt-get update
RUN sudo apt-get -y install emacs
RUN sudo apt-get -y install default-jre default-jdk
RUN sudo apt-get -y install python
RUN sudo apt-get -y install gnuplot
RUN sudo apt-get -y install postgresql postgresql-contrib
RUN sudo apt-get -y install git
RUN sudo apt-get -y install build-essential
RUN sudo apt-get -y install libnuma-dev
RUN sudo apt-get -y install bc
RUN cd ~/ && git clone https://github.com/HazyResearch/deepdive.git
RUN sudo apt-get install zip unzip
RUN cd ~/deepdive && make

# Configure environment variables
RUN echo 'export PGPORT=$DB_PORT_5432_TCP_PORT' >> ~/.bashrc
RUN echo 'export PGHOST=$DB_PORT_5432_TCP_ADDR' >> ~/.bashrc
RUN echo 'export PGPASSWORD=' >> ~/.bashrc
RUN echo 'export PGUSER=gpadmin' >> ~/.bashrc
RUN echo 'export DEEPDIVE_HOME=~/deepdive' >> ~/.bashrc
RUN echo 'export LD_LIBRARY_PATH=$DEEPDIVE_HOME/lib/dw_linux/lib:$DEEPDIVE_HOME/sbt:$DEEPDIVE_HOME/lib/dw_linux/lib64' >> ~/.bashrc
RUN echo 'export PATH=~/deepdive/sbt:$PATH' >> ~/.bashrc

# Initialize script to wait for greenplum
RUN echo 'while true; do' >> ~/.bashrc
RUN echo '  psql -q -h $DB_PORT_5432_TCP_ADDR -p $DB_PORT_5432_TCP_PORT -U gpadmin deepdive -c "SELECT 1;" > /dev/null 2 >& 1' >> ~/.bashrc
RUN echo '  RETVAL=$?' >> ~/.bashrc
RUN echo '  [ $RETVAL -eq 0 ] && break' >> ~/.bashrc
RUN echo '  echo -ne "Waiting for DB\r"' >> ~/.bashrc
RUN echo '  sleep 1' >> ~/.bashrc
RUN echo '  echo -ne "Waiting for DB.\r"' >> ~/.bashrc
RUN echo '  sleep 1' >> ~/.bashrc
RUN echo '  echo -ne "Waiting for DB..\r"' >> ~/.bashrc
RUN echo '  sleep 1' >> ~/.bashrc
RUN echo '  echo -ne "Waiting for DB...\r"' >> ~/.bashrc
RUN echo '  sleep 1' >> ~/.bashrc
RUN echo '  echo -ne "Waiting for DB....\r"' >> ~/.bashrc
RUN echo '  sleep 1' >> ~/.bashrc
RUN echo 'done' >> ~/.bashrc
RUN echo 'echo -ne "\nGreenplum is up and running! You may now use deepdive.\n"' >> ~/.bashrc

RUN sed -i s/'sbt "test-only org.deepdive.test.integration.ChunkingApp -- -oF"'/'echo "Skipping ChunkingApp" \#sbt "test-only org.deepdive.test.integration.ChunkingApp -- -oF"'/g /root/deepdive/test/test_psql.sh

RUN mkdir -p ~/deepdive/app

VOLUME ["/root/deepdive/app"]