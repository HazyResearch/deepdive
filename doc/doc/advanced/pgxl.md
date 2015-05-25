---
layout: default
---

# Using DeepDive with Postgres-XL 

This document describes how to install and configure
[Postgres-XL](http://www.postgres-xl.org/) to work
with DeepDive. It also describes one caveat needed in writing queries when using
XL, and some [FAQs](#faq) about using XL.

After installing XL, DeepDive should work well with it. Apart from the
following caveat below, you should not observe any difference than if you
were running PostgreSQL.

## Important Caveat

You should add a `DISTRIBUTE BY HASH` clause in all `CREATE TABLE` commands. **Do
not use the column `id`** as the distribution key. **Do not use** a distribution
key that is **not initially assigned**.

Refer to the [XL
manual](http://files.postgres-xl.org/documentation/) for
more information.

## Installation
We now describe how to install XL and configure it to be used with
DeepDive. The steps were tested to install XL on Ubunut 15.04. 

### Installation Notes
The following is a list of alternative steps that you may have to take in case
your environment is more complex than usual.

- If you do not have write permissions to the `/usr/local/` directory,  let
  `GREENPLUM_DIR` be the directory where you wish to install Greenplum and in
  the steps below, replace `/usr/local/` with `GREENPLUM_DIR`.

- If you are installing Greenplum on a machine without root access, skip the
  step **Setting the Greenplum Recommended OS Parameters**.

- You will be prompted to enter your password to `localhost` several times
  throughout the installation process.

- If you can not change your settings to login to localhost without a password,
  skip the step **Configure ssh with localhost**.

- If you do not have write permissions to the ~/.bashrc file, in the steps **Set
  Greenplum related session variables** and **Configure PATH to add master data
  directory**,  simply run the commands `source
  GREENPLUM_DIR/greenplum-db/greenplum_path.sh` and `export
  MASTER_DATA_DIRECTORY=GREENPLUM_DIR/greenplumdb/master/gpsne-1`. In order to
  have these commands run every time you log in to the system, save a new script
  called `greenplum_startup.sh` with the following contents:

	```bash
	source GREENPLUM_DIR/greenplum-db/greenplum_path.sh
	export MASTER_DATA_DIRECTORY=GREENPLUM_DIR/greenplumdb/master/gpsne-1
	```
	and edit your shell settings so that this script gets executed every time
	the shell opens (or remember to run it).


### Setting OS Parameters

Set the following parameters in the `/etc/sysctl.conf` file: 
```
kernel.sem = 1000  32000  32  1000
kernel.shmmax = 429496729600  #400GB
```

After making these changes, run
```
sudo sysctl -p /etc/sysctl.conf
```

Set the following parameter in the `/etc/ssh/ssh_config` file:
```
StrictHostKeyChecking no
```

Finally, set the following parameter in the `/etc/ssh/sshd_config` file:
```
MaxStartups 100
```
After that, run
```
sudo service ssh restart
```

### Building XL from Source

```
sudo apt-get update
sudo apt-get -y install -y screen curl git rsync openssl locales openssh-server openssh-client
sudo apt-get -y install -y gcc flex bison make cmake jade openjade docbook docbook-dsssl
sudo apt-get -y install zlib1g-dev libreadline6-dev python-dev libssl-dev
sudo localedef -i en_US -c -f UTF-8 -A /usr/share/locale/locale.alias en_US.UTF-8
```

Now set `$TARGET_DIR` to the directory into which you would like to install XL.
```
TARGET_DIR=/opt/pgxl
```

Then add the following line(s) to `/etc/ld.so.conf` (replace $TARGET_DIR with dir):
```
/usr/local/lib
$TARGET_DIR/lib
```

The remaining parts of the installation do not require sudo rights. However, make sure
the user account doing the installation has write permissions to $TARGET_DIR.

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

### Set XL related session variables

To set Greenplum related session variables, modify your
`~/.bashrc` script and add the line:

```bash
export PATH=$TARGET_DIR/bin:\$PATH
export PGXC_CTL_HOME=$TARGET_DIR/conf
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
**Be sure that you have write permission to these directory**.

```bash
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

Create a file `$TARGET_DIR/conf/pgxc_ctl.conf` with the following contents (replace
variables with their values):

```
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
```

Above configuration creates a cluster with two data nodes, one coordinator,
and one manager.

### Configure PATH

Configure the following paths into your `~/.bashrc`:

```bash
export PATH=$TARGET_DIR/bin:$PATH
export PGXC_CTL_HOME=$TARGET_DIR/conf
```

Run `source ~/.bashrc`.

### Start the database server

Execute the following command to launch the database server

```bash
# create all nodes
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
<!--
- **How do I enable fuzzy string match / install "contrib" module in Greenplum?**

	To enable *fuzzystringmatch* / the *contrib* module available for PostgreSQL
	for Greenplum, see [this
	link](http://blog.2ndquadrant.com/fuzzystrmatch_greenplum/).
-->
