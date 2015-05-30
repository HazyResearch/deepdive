#!/bin/bash

# Single-host setup of a new vanilla Postgres server.
# Tested on Ubuntu 15.04.
# Run it as a non-root user with sudo permission.
# The PG data and processes will be owned by that user.

# This script is a shameless knock-off of the PGXL script.


###################################################
# PLEASE EDIT THIS SECTION TO FIT YOUR MACHINE SPEC,
# ESPECIALLY DATA_DIR, NUM_DATA_NODES, and DATA_NODE_SHARED_BUFFERS.
DATA_DIR=/data  # change this to the data volume
USER_DB_PORT=6432
KERNAL_SHMMAX=429496729600  #400GB

###################################################


###################################################
# Typically you can leave this section alone
BUILD_DIR=~/pg_install
TARGET_DIR=/opt/pg
###################################################


sudo apt-get update
sudo apt-get -y install -y screen curl git rsync openssl locales openssh-server openssh-client
sudo apt-get -y install -y gcc flex bison make cmake jade openjade docbook docbook-dsssl
sudo apt-get -y install zlib1g-dev libreadline6-dev python-dev libssl-dev
sudo localedef -i en_US -c -f UTF-8 -A /usr/share/locale/locale.alias en_US.UTF-8

sudo mkdir -p $TARGET_DIR
sudo chown $USER:$USER $TARGET_DIR
sudo mkdir -p $DATA_DIR
sudo chown $USER:$USER $DATA_DIR

echo "export PGDATA=$DATA_DIR/pg" | sudo tee -a /etc/profile

#echo "/usr/local/lib" | sudo tee -a /etc/ld.so.conf
#echo "$TARGET_DIR/lib" | sudo tee -a /etc/ld.so.conf

echo "kernel.shmmax = $KERNAL_SHMMAX" | sudo tee -a /etc/sysctl.conf
sudo sysctl -p /etc/sysctl.conf

echo "   StrictHostKeyChecking no" | sudo tee -a /etc/ssh/ssh_config
sudo service ssh restart


mkdir -p $TARGET_DIR/conf

# insert new PATH to beginning of .bashrc so that it's picked up by ssh
if [ -f ~/.bashrc ]; then
    sed -i.old "1s;^;export PATH=$TARGET_DIR/bin:\$PATH\n\n;" ~/.bashrc
else
    echo "1s;^;export PATH=$TARGET_DIR/bin:\$PATH\n\n;" > ~/.bashrc
fi
source ~/.bashrc


mkdir -p ~/.ssh
chmod 700 ~/.ssh
cd ~/.ssh
if [ ! -f id_rsa.pub ]; then
    ssh-keygen -t rsa -N "" -f id_rsa
fi
cat id_rsa.pub >> authorized_keys
chmod 600 authorized_keys


mkdir -p $BUILD_DIR
cd $BUILD_DIR
wget -O postgresql-9.4.1.tar.gz https://ftp.postgresql.org/pub/source/v9.4.1/postgresql-9.4.1.tar.gz
tar -xzf postgresql-9.4.1.tar.gz
cd $BUILD_DIR/postgresql-9.4.1
./configure --with-python --with-openssl --prefix $TARGET_DIR
make -j 4
make install

pg_ctl init
pg_ctl start

echo "CONGRATS! NOW PG IS OPEN FOR BUSINESS on port $USER_DB_PORT!"

