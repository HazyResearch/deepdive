#!/bin/bash

# Single-host setup of a new Postgres-XL server.
# Tested on Ubuntu 14.04.
# Run it as a non-root user with sudo permission.
# The PG data and processes will be owned by that user.
# There will be 1 gtm, 1 coordinator, and NUM_DATA_NODES data nodes.
#
# http://files.postgres-xl.org/documentation/installation.html
# http://files.postgres-xl.org/documentation/server-start.html
# http://files.postgres-xl.org/documentation/pgxc_ctl.html
# http://blog.jelly-king.com/prog/2015/03/22/Install%20pgxl%20on%20cluster.html
# https://github.com/postmind-net/pgxl-docker/blob/master/pgxl/Dockerfile

# TODO:
# - figure out how to tune remote_query_cost, network_byte_cost, and sequence_range
#   http://files.postgres-xl.org/documentation/pg-xc-specifics.html

###################################################
# PLEASE EDIT THIS SECTION TO FIT YOUR MACHINE SPEC,
# ESPECIALLY DATA_DIR, NUM_DATA_NODES, and DATA_NODE_SHARED_BUFFERS.
DATA_DIR=/mnt/pgxl/nodes  # change this to the data volume
USER_DB_PORT=5432
KERNAL_SHMMAX=429496729600  #400GB
NUM_DATA_NODES=2
MAX_USER_CONNECTIONS=100
DATA_NODE_SHARED_BUFFERS="2000MB"
DATA_NODE_WORK_MEM="128MB"
DATA_NODE_MAINTENANCE_MEM="128MB"
DATA_NODE_WAL_BUFFERS="16MB"
DATA_NODE_CHECKPOINT_SEGMENTS="256"
###################################################


###################################################
# Typically you can leave this section alone
BUILD_DIR=~/pgxl_install
TARGET_DIR=/opt/pgxl
###################################################

# ask for confirmation of key settings
read -p "Enter number of data nodes [$NUM_DATA_NODES]: " CUSTOM_NUM_DATA_NODES
NUM_DATA_NODES=${CUSTOM_NUM_DATA_NODES:-$NUM_DATA_NODES}
read -p "Enter data dir to hold segment data [$DATA_DIR]: " CUSTOM_DATA_DIR
DATA_DIR=${CUSTOM_DATA_DIR:-$DATA_DIR}
read -p "Enter port number [$USER_DB_PORT]: " CUSTOM_USER_DB_PORT
USER_DB_PORT=${CUSTOM_USER_DB_PORT:-$USER_DB_PORT}

sudo apt-get update
sudo apt-get -y install -y screen curl git rsync openssl locales openssh-server openssh-client
sudo apt-get -y install -y gcc flex bison make cmake jade openjade docbook docbook-dsssl
sudo apt-get -y install zlib1g-dev libreadline6-dev python-dev libssl-dev
sudo localedef -i en_US -c -f UTF-8 -A /usr/share/locale/locale.alias en_US.UTF-8

sudo mkdir -p $TARGET_DIR
sudo chown $USER:$USER $TARGET_DIR
sudo mkdir -p $DATA_DIR
sudo chown $USER:$USER $DATA_DIR


echo "export PGXC_CTL_HOME=$TARGET_DIR/conf" | sudo tee -a /etc/profile
export PGXC_CTL_HOME=$TARGET_DIR/conf

echo "/usr/local/lib" | sudo tee -a /etc/ld.so.conf
echo "$TARGET_DIR/lib" | sudo tee -a /etc/ld.so.conf

echo "kernel.shmmax = $KERNAL_SHMMAX" | sudo tee -a /etc/sysctl.conf
sudo sysctl -p /etc/sysctl.conf

echo "   StrictHostKeyChecking no" | sudo tee -a /etc/ssh/ssh_config
sudo service ssh restart

# must add newline here
echo "
MaxStartups 100" | sudo tee -a /etc/ssh/sshd_config
sudo service ssh restart
echo '
kernel.sem = 1000  32000  32  1000' | sudo tee -a /etc/sysctl.conf
sudo sysctl -w kernel.sem="1000  32000  32  1000"


mkdir -p $TARGET_DIR/conf

# insert new PATH to beginning of .bashrc so that it's picked up by ssh
if [ -f ~/.bashrc ]; then
    sed -i.old "1s;^;export PATH=$TARGET_DIR/bin:\$PATH\n\n;" ~/.bashrc
else
    echo "1s;^;export PATH=$TARGET_DIR/bin:\$PATH\n\n;" > ~/.bashrc
fi
export PATH="$TARGET_DIR/bin:$PATH"


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
wget -O pgxl-v9.2.tar.gz http://sourceforge.net/projects/postgres-xl/files/Releases/Version_9.2rc/postgres-xl-v9.2-src.tar.gz/download
tar -xzf pgxl-v9.2.tar.gz

cd $BUILD_DIR/postgres-xl/
./configure --with-python --with-openssl --prefix $TARGET_DIR
make -j 4
make install

for pkg in btree_gin btree_gist earthdistance fuzzystrmatch hstore intagg intarray oid2name \
pg_buffercache pgcrypto pgxc_clean pgxc_ctl pgxc_ddl pgxc_monitor stormstats \
tablefunc tsearch2 unaccent; do
    cd $BUILD_DIR/postgres-xl/contrib/$pkg
    make || continue
    make install
done


datanodeMasterDirs=()
datanodeNames=()
datanodeMasterServers=()
datanodePorts=()
datanodePoolerPorts=()
datanodeMaxWALSenders=()
datanodeSpecificExtraConfig=()
datanodeSpecificExtraPgHba=()

for i in $(seq 1 $NUM_DATA_NODES); do
    datanodeMasterDirs+=($DATA_DIR/data${i})
    datanodeNames+=(data${i})
    datanodeMasterServers+=(localhost)
    datanodePorts+=(`expr 3000 + $i`)
    datanodePoolerPorts+=(`expr 4000 + $i`)
    datanodeMaxWALSenders+=(5)
    datanodeSpecificExtraConfig+=(none)
    datanodeSpecificExtraPgHba+=(none)
done


cat <<EOT > $TARGET_DIR/conf/pgxc_ctl.conf
pgxcOwner=$USER
pgxcUser=\$pgxcOwner
tmpDir=/tmp
localTmpDir=\$tmpDir
configBackup=n
pgxcInstallDir=$TARGET_DIR

gtmName=gtm
gtmMasterServer=localhost
gtmMasterPort=20001
gtmMasterDir=$DATA_DIR/gtm
gtmSlave=n
gtmProxy=n

coordMasterDirs=($DATA_DIR/coord)
coordNames=(coord)
coordMasterServers=(localhost)
coordPorts=($USER_DB_PORT)
poolerPorts=(20002)
coordMaxWALSenders=(5)
coordSlave=n

datanodeMasterDirs=(${datanodeMasterDirs[@]})
datanodeNames=(${datanodeNames[@]})
datanodeMasterServers=(${datanodeMasterServers[@]})
datanodePorts=(${datanodePorts[@]})
datanodePoolerPorts=(${datanodePoolerPorts[@]})
datanodeMaxWALSenders=(${datanodeMaxWALSenders[@]})
datanodeSpecificExtraConfig=(${datanodeSpecificExtraConfig[@]})
datanodeSpecificExtraPgHba=(${datanodeSpecificExtraPgHba[@]})
datanodeSlave=n

coordExtraConfig=coordExtraConfig
cat > \$coordExtraConfig <<EOF
#================================================
# Added to all the coordinator postgresql.conf
log_destination = 'stderr'
logging_collector = on
log_directory = 'logs'
listen_addresses = '*'
log_filename = 'coordinator.log'
max_connections = $MAX_USER_CONNECTIONS
shared_buffers = $DATA_NODE_SHARED_BUFFERS
checkpoint_segments = $DATA_NODE_CHECKPOINT_SEGMENTS
work_mem = $DATA_NODE_WORK_MEM
maintenance_work_mem = $DATA_NODE_MAINTENANCE_MEM
wal_buffers = $DATA_NODE_WAL_BUFFERS
EOF

datanodeExtraConfig=datanodeExtraConfig
cat > \$datanodeExtraConfig <<EOF
#================================================
# Added to all the datanode postgresql.conf
log_destination = 'stderr'
logging_collector = on
log_directory = 'logs'
log_filename = 'datanode.log'
max_connections = $MAX_USER_CONNECTIONS
shared_buffers = $DATA_NODE_SHARED_BUFFERS
checkpoint_segments = $DATA_NODE_CHECKPOINT_SEGMENTS
work_mem = $DATA_NODE_WORK_MEM
maintenance_work_mem = $DATA_NODE_MAINTENANCE_MEM
wal_buffers = $DATA_NODE_WAL_BUFFERS
EOF
EOT

# create all nodes
pgxc_ctl "init all"
# check out their status
pgxc_ctl "monitor all"
# try dummy query
psql -p $USER_DB_PORT postgres -c "select version()"

psql -p $USER_DB_PORT template1 -c "create extension plpythonu"
psql -p $USER_DB_PORT template1 -c "create extension intarray"
psql -p $USER_DB_PORT template1 -c "create extension hstore"

echo "CONGRATS! NOW PGXL IS OPEN FOR BUSINESS on port $USER_DB_PORT!"

