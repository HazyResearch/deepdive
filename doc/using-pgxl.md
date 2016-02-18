---
layout: default
title: Using DeepDive with Postgres-XL
---

# Using DeepDive with Postgres-XL

This document describes how to install and configure
[Postgres-XL](http://www.postgres-xl.org/) to work
with DeepDive. It also describes two caveats needed in writing queries when using
XL, and some [FAQs](#faq) about using XL.

After installing XL, DeepDive should work well with it. Apart from the
following caveat below, you should not observe any difference than if you
were running PostgreSQL.

## Important caveats

First, you should add a `DISTRIBUTE BY HASH` clause in all `CREATE TABLE` commands. **Do
not use the column `id`** as the distribution key. **Do not use** a distribution
key that is **not initially assigned**.

Refer to the [XL
manual](http://files.postgres-xl.org/documentation/) for
more information.

Second, you should always create tables as `CREATE UNLOGGED TABLE ...`. The unlogged
keyword turns off write-ahead logging (WAL), and gives a significant speedup
in writes (often 10X). WAL is not needed for DeepDive apps, because the
database is only used for processing, not for persisting data.

For more details on XL-specific SQL queries in a DeepDive example application,
see [this example](https://github.com/HazyResearch/deepdive/tree/master/examples/spouse_example).

## Installation
We now describe how to install XL and configure it to be used with
DeepDive. The steps were tested to install XL on Ubuntu 15.04.

We assume that the user executing these commands has sudo rights.

### Setting OS parameters

Set the following parameters in the `/etc/sysctl.d/50-pgxl.conf` file:

```
sudo tee /etc/sysctl.d/50-pgxl.conf <<EOF
kernel.sem = 1000  32000  32  1000
# up to 400GB shared memory
kernel.shmmax = 429496729600
EOF
```

After making these changes, run

```bash
sudo sysctl --system
```

Finally, adjust ssh settings in `/etc/ssh/ssh_config` and `/etc/ssh/sshd_config` files:

```
sudo tee -a /etc/ssh/ssh_config <<EOF
StrictHostKeyChecking no
EOF
sudo tee -a /etc/ssh/sshd_config <<EOF
MaxStartups 100
EOF
```
After that, run

```bash
sudo service ssh restart
```

### Building XL from source

```bash
sudo apt-get update
sudo apt-get install -y screen curl git rsync openssl locales openssh-server openssh-client \
                        build-essential gcc flex bison make cmake jade openjade docbook docbook-dsssl \
                        zlib1g-dev libreadline6-dev python-dev libssl-dev
sudo localedef -i en_US -c -f UTF-8 -A /usr/share/locale/locale.alias en_US.UTF-8
```

Now set `$TARGET_DIR` to the directory into which you would like to install XL.

```bash
TARGET_DIR=/opt/pgxl
sudo mkdir -p $TARGET_DIR
sudo chown $USER $TARGET_DIR
```

Then create a file `/etc/ld.so.conf.d/pgxl.conf` for the `$TARGET_DIR`:

```bash
sudo tee /etc/ld.so.conf.d/pgxl.conf <<EOF
$TARGET_DIR/lib
EOF
```

The remaining parts of the installation do not require sudo rights. However, make sure
the user account doing the installation has write permissions to `$TARGET_DIR`.

Create a build directory and download the XL sources.

```bash
BUILD_DIR=~/pgxl_install
mkdir -p $BUILD_DIR
cd $BUILD_DIR
wget -O pgxl-v9.2.tar.gz http://sourceforge.net/projects/postgres-xl/files/Releases/Version_9.2rc/postgres-xl-v9.2-src.tar.gz/download
tar -xzf pgxl-v9.2.tar.gz
```

Now, you can build the sources.

```bash
cd $BUILD_DIR/postgres-xl/
./configure --with-python --with-openssl --prefix $TARGET_DIR
make -j 4
make install
```

Additionally modules can be added as follows.

```bash
for pkg in btree_gin btree_gist earthdistance fuzzystrmatch hstore intagg intarray oid2name \
pg_buffercache pgcrypto pgxc_clean pgxc_ctl pgxc_ddl pgxc_monitor stormstats \
tablefunc tsearch2 unaccent; do
    cd $BUILD_DIR/postgres-xl/contrib/$pkg
    make; make install
done
```

### Configure ssh with localhost

Now you need to generate ssh keys for `localhost`. Run:

```bash
mkdir -p ~/.ssh
chmod 700 ~/.ssh
cd ~/.ssh
if [ ! -f id_rsa.pub ]; then
    ssh-keygen -t rsa -N "" -f id_rsa
fi
cat id_rsa.pub >> authorized_keys
chmod 600 authorized_keys
```

Then you should be able to `ssh` into `localhost` without password, and you can
move on.

### Create directories for the database

Now set `$DATA_DIR` to the directory, where the database files will be stored.
**Be sure that you have write permission to this directory**.

```bash
DATA_DIR=$TARGET_DIR/data
mkdir -p $DATA_DIR
```

### Configure the XL database

In order to run XL, we still need to create a file with configuration settings. Run

```bash
mkdir -p $TARGET_DIR/conf
```

Now, assuming that `$USER` is set to the account under which pgxl should execute, the
following settings should provide a good starting point.

```bash
USER_DB_PORT=5432
MAX_USER_CONNECTIONS=100
DATA_NODE_SHARED_BUFFERS="2000MB"
DATA_NODE_WORK_MEM="128MB"
DATA_NODE_MAINTENANCE_MEM="128MB"
DATA_NODE_WAL_BUFFERS="16MB"
DATA_NODE_CHECKPOINT_SEGMENTS="256"
```

Now create a file `$TARGET_DIR/conf/pgxc_ctl.conf` as follows:

```
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

datanodeMasterDirs=($DATA_DIR/data1 $DATA_DIR/data2)
datanodeNames=(data1 data2)
datanodeMasterServers=(localhost localhost)
datanodePorts=(3001 3002)
datanodePoolerPorts=(4001 4002)
datanodeMaxWALSenders=(5 5)
datanodeSpecificExtraConfig=(none none)
datanodeSpecificExtraPgHba=(none none)
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
```

Above configuration creates a cluster with two data nodes, one coordinator,
and one manager.

### Configure PATH

Configure the necessary paths into your `~/.bashrc`, so they're effective via ssh:

```bash
( echo 1i  # insert following lines at the beginning of bashrc
cat <<EOF
export PATH=$TARGET_DIR/bin:\$PATH
export PGXC_CTL_HOME=$TARGET_DIR/conf
EOF
echo .; echo wq ) | ed ~/.bashrc
```

Run `source ~/.bashrc`.

### Start the database server

Execute the following command to launch the database server

```bash
pgxc_ctl "init all"
```

Run the following to complete the installation of extensions:

```
psql -p $USER_DB_PORT template1 -c "create extension plpythonu"
psql -p $USER_DB_PORT template1 -c "create extension intarray"
psql -p $USER_DB_PORT template1 -c "create extension hstore"
```

### Verify the installation

Follow these commands and you should get similar output.

```
$ psql postgres

psql (8.2.15)
Type “help” for help.

postgres=#

postgres=# \l

                List of databases
   Name    | Owner | Encoding | Access privileges
-----------+-------+----------+-------------------
 postgres  | Xxx   | UTF8     |
 template0 | Xxx   | UTF8     | =c/Xxx
                              : Xxx=CTc/Xxx
 template1 | Xxx   | UTF8     | =c/Xxx
                              : Xxx=CTc/Xxx
(3 rows)

postgres=# \q
```

### Stop and Start the database server

Use `pgxc_ctl "stop all"` and `pgxc_ctl "start all"` to stop / start the XL server at any time.

## <a name="faq" href="#"></a> FAQs
- **How do I enable fuzzy string match / install "contrib" module in XL?**

        To enable *fuzzystringmatch* or another *contrib* module available for XL,
        see above build instructions on how to build XL with extensions.
- **Can I create a cluster with N nodes?**

        You certainly can. Note that for clusters larger than 16 nodes you may need
        to adjust certain configuration parameters, especially buffer sizes (so that
        you don't run out of memory) and kernel settings (so that the system can open
        enough SSH sessions).
