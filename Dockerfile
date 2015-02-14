FROM ubuntu
MAINTAINER adamwgoldberg@gmail.com
RUN sudo apt-get update
RUN sudo apt-get install -y gnuplot python libpython2.7-dev default-jre default-jdk emacs ruby-full
RUN sudo apt-get -y install postgresql postgresql-contrib git build-essential libnuma-dev bc zip unzip
RUN gem install jekyll
RUN cd ~/ && git clone https://github.com/HazyResearch/deepdive.git
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
RUN echo 'echo "DeepDive needs a database connection to run and is waiting for the DB to finish initializing. After it finishes, the shell will return control to you."' >> ~/.bashrc
RUN echo 'while true; do' >> ~/.bashrc
RUN echo '  psql -q -h $DB_PORT_5432_TCP_ADDR -p $DB_PORT_5432_TCP_PORT -U gpadmin deepdive -c "SELECT 1;" > /dev/null 2>&1' >> ~/.bashrc
RUN echo '  RETVAL=$?' >> ~/.bashrc
RUN echo '  [ $RETVAL -eq 0 ] && break' >> ~/.bashrc
RUN echo '  echo -ne "DeepDive is waiting for the DB to finish initializing\r"' >> ~/.bashrc
RUN echo '  sleep 1' >> ~/.bashrc
RUN echo '  echo -ne "DeepDive is waiting for the DB to finish initializing.\r"' >> ~/.bashrc
RUN echo '  sleep 1' >> ~/.bashrc
RUN echo '  echo -ne "DeepDive is waiting for the DB to finish initializing..\r"' >> ~/.bashrc
RUN echo '  sleep 1' >> ~/.bashrc
RUN echo '  echo -ne "DeepDive is waiting for the DB to finish initializing...\r"' >> ~/.bashrc
RUN echo '  sleep 1' >> ~/.bashrc
RUN echo 'done' >> ~/.bashrc
RUN echo 'echo -ne "\nGreenplum is up and running! You may now use deepdive.\n"' >> ~/.bashrc

RUN sed -i s/'sbt "test-only org.deepdive.test.integration.ChunkingApp -- -oF"'/'echo "Skipping ChunkingApp" \#sbt "test-only org.deepdive.test.integration.ChunkingApp -- -oF"'/g /root/deepdive/test/test_psql.sh

RUN mkdir -p ~/deepdive/app

VOLUME ["/root/deepdive/app"]
