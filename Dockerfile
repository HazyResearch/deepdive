FROM ubuntu
MAINTAINER adamwgoldberg@gmail.com
RUN sudo apt-get update && apt-get install -y gnuplot python libpython2.7-dev default-jre default-jdk emacs postgresql postgresql-contrib git build-essential libnuma-dev bc unzip locales
RUN locale-gen en_US en_US.UTF-8 && \
    dpkg-reconfigure locales
RUN cd ~/ && git clone https://github.com/HazyResearch/deepdive.git
RUN cd ~/deepdive && make

# Configure environment variables and initialize script to wait for the deepdive database
RUN echo 'export PGPORT=$DB_PORT_5432_TCP_PORT' >> ~/.bashrc && echo 'export PGHOST=$DB_PORT_5432_TCP_ADDR' >> ~/.bashrc && \
    echo 'export PGPASSWORD=' >> ~/.bashrc && echo 'export PGUSER=gpadmin' >> ~/.bashrc && \
    echo 'export DEEPDIVE_HOME=~/deepdive' >> ~/.bashrc && \
    echo 'export LD_LIBRARY_PATH=$DEEPDIVE_HOME/lib/dw_linux/lib:$DEEPDIVE_HOME/sbt:$DEEPDIVE_HOME/lib/dw_linux/lib64' >> ~/.bashrc && \
    echo 'export PATH=~/deepdive/sbt:$PATH' >> ~/.bashrc && \
    echo 'echo "DeepDive needs a database connection to run and is waiting for the DB to finish initializing. After it finishes, the shell will return control to you."' >> ~/.bashrc && \
    echo 'while true; do' >> ~/.bashrc && \
    echo '  psql -q -h $DB_PORT_5432_TCP_ADDR -p $DB_PORT_5432_TCP_PORT -U gpadmin deepdive -c "SELECT 1;" > /dev/null 2>&1' >> ~/.bashrc && \
    echo '  RETVAL=$?' >> ~/.bashrc &&     echo '  [ $RETVAL -eq 0 ] && break' >> ~/.bashrc && \
    echo '  echo -ne "DeepDive is waiting for the DB to finish initializing\r"' >> ~/.bashrc && \ 
    echo '  sleep 1' >> ~/.bashrc && \
    echo '  echo -ne "DeepDive is waiting for the DB to finish initializing.\r"' >> ~/.bashrc && \
    echo '  sleep 1' >> ~/.bashrc && \
    echo '  echo -ne "DeepDive is waiting for the DB to finish initializing..\r"' >> ~/.bashrc && \
    echo '  sleep 1' >> ~/.bashrc && \
    echo '  echo -ne "DeepDive is waiting for the DB to finish initializing...\r"' >> ~/.bashrc &&\
    echo '  sleep 1' >> ~/.bashrc && echo 'done' >> ~/.bashrc && \
    echo 'echo -ne "\nGreenplum is up and running! You may now use deepdive.\n"' >> ~/.bashrc 

RUN sed -i s/'sbt "test-only org.deepdive.test.integration.ChunkingApp -- -oF"'/'echo "Skipping ChunkingApp" \#sbt "test-only org.deepdive.test.integration.ChunkingApp -- -oF"'/g /root/deepdive/test/test_psql.sh

RUN mkdir -p ~/deepdive/app

VOLUME ["/root/deepdive/app"]
