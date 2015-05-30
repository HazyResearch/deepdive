#!/bin/bash

# Single-host setup of a new vanilla Postgres server.
# Tested on Ubuntu 15.04.
# Run it as a non-root user with sudo permission.
# The PG data and processes will be owned by that user.

# This script is a shameless knock-off of the PGXL script.


###################################################
# PLEASE EDIT THIS SECTION TO FIT YOUR MACHINE SPEC,
# ESPECIALLY DATA_DIR, NUM_DATA_NODES, and DATA_NODE_SHARED_BUFFERS.
DATA_DIR=/data  # change this to the data volume, installer will write to DATA_DIR/pg
USER_DB_PORT=6432
###################################################


###################################################
# Typically you can leave this section alone
BUILD_DIR=~/pg_install
TARGET_DIR=/opt/pg
###################################################

mkdir -p $DATA_DIR
mkdir -p $TARGET_DIR/conf

# insert new PATH to beginning of .bashrc so that it's picked up by ssh
if [ -f ~/.bashrc ]; then
    sed -i.old "1s;^;export PATH=$TARGET_DIR/bin:\$PATH\n\n;" ~/.bashrc
else
    echo "1s;^;export PATH=$TARGET_DIR/bin:\$PATH\n\n;" > ~/.bashrc
fi
echo "export PGDATA=$DATA_DIR/pg" | tee -a ~/.bashrc
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

echo "CONGRATS! PG IS NOW INSTALLED."
echo "LOG OUT AND LOG IN AGAIN. THEN RUN:"
echo ""
echo "   $TARGET_DIR/bin/pg_ctl init
echo "   $TARGET_DIR/bin/pg_ctl -o '-p 6432' start"
echo ""
#pg_ctl init
#pg_ctl start

#echo "CONGRATS! NOW PG IS OPEN FOR BUSINESS on port $USER_DB_PORT!"



