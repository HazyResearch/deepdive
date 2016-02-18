---
layout: default
title: Using DeepDive with Greenplum
---

# Using DeepDive with Greenplum

This document describes how to install and configure [Greenplum](http://pivotal.io/big-data/pivotal-greenplum) to work with DeepDive.
It describes some [specifications](#using_greenplum) when using Greenplum, how to [install](#installing_greenplum) and some [FAQs](#faq) about using Greenplum.

After installing Greenplum, DeepDive should work well with it and no difference should be observed from running PostgreSQL, except an increase in speed.

## <a name="using_greenplum" href="#"></a> Greenplum specific configuration

### Distributed by clauses

For Greenplum to work optimally, `DISTRIBUTED BY` clauses should be added in all the tables declarations.
For that, when declaring a table in `app.ddlog`, the annotation `@distributed_by` must be added in front of the column for which the table should be distributed by.
For instance, in the spouse example, to distribute the table `sentences` by the column `doc_id` (in addition to other annotations), the following declaration should be written:

```
sentences(
    @key
    @distributed_by
    doc_id         text,
    @key
    sentence_index int,
    sentence_text  text,
    tokens         text[],
    lemmas         text[],
    pos_tags       text[],
    ner_tags       text[],
    doc_offsets    int[],
    dep_types      text[],
    dep_tokens     int[]
).
```
As seen in this example, the annotation `@distributed_by` can be easily added to other existing annotations.
If DeepDive is run with PostgreSQL, the annotations `@distributed_by` will simply be ignored, allowing the same `app.ddlog` to be easily run under PostgreSQL and Greenplum.

The `db.url` should explicit that Greenplum is used, being for instance `greenplum://localhost:6432/spouse`.

Refer to the [Greenplum manual](http://media.gpadmin.me/wp-content/uploads/2012/11/GPDBAGuide.pdf) for more information.

### Parallel unloading and grounding

Greenplum allows DeepDive to speed up by loading and unloading data in parallel. The number of parallel processes can be manually fixed by setting the environment variable `DEEPDIVE_NUM_PARALLEL_UNLOADS=...`

DeepDive will automatically use Greenplum's file system server `gpfdist` to speed up the grounding. If the parallel grounding should be disabled, export the environment variable `GPFDIST_DISABLE=true`.


## <a name="installing_greenplum" href="#"></a> Installation

<!--
<todo>Update to install from source, Migrate <https://github.com/HazyResearch/greenplum-howto></todo>
-->

We now describe how to install Greenplum and configure it to be used with
DeepDive. The steps were tested to install Greenplum on CentOS 5.6 x64. For
demonstration purposes only, the presentation is limited to the single-node mode
of Greenplum. Everything should work also with the multi-node configuration.


### Installation notes
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


### Setting the Greenplum recommended OS parameters

Set the following parameters in the `/etc/sysctl.conf` file and reboot:

```
xfs_mount_options = rw,noatime,inode64,allocsize=16m
sysctl.kernel.shmmax = 500000000
sysctl.kernel.shmmni = 4096
sysctl.kernel.shmall = 4000000000
sysctl.kernel.sem = 250 512000 100 2048
sysctl.kernel.sysrq = 1
sysctl.kernel.core_uses_pid = 1
sysctl.kernel.msgmnb = 65536
sysctl.kernel.msgmax = 65536
sysctl.kernel.msgmni = 2048
sysctl.net.ipv4.tcp_syncookies = 1
sysctl.net.ipv4.ip_forward = 0
sysctl.net.ipv4.conf.default.accept_source_route = 0
sysctl.net.ipv4.tcp_tw_recycle = 1
sysctl.net.ipv4.tcp_max_syn_backlog = 4096
sysctl.net.ipv4.conf.all.arp_filter = 1
sysctl.net.ipv4.ip_local_port_range = 1025 65535
sysctl.net.core.netdev_max_backlog = 10000
sysctl.vm.overcommit_memory = 2
```

*Note:* For RHEL version 6.x platforms, the above parameters do not include the
`sysctl.` prefix, as follows:

```
xfs_mount_options = rw,noatime,inode64,allocsize=16m
kernel.shmmax = 500000000
kernel.shmmni = 4096
kernel.shmall = 4000000000
kernel.sem = 250 512000 100 2048
kernel.sysrq = 1
kernel.core_uses_pid = 1
kernel.msgmnb = 65536
kernel.msgmax = 65536
kernel.msgmni = 2048
net.ipv4.tcp_syncookies = 1
net.ipv4.ip_forward = 0
net.ipv4.conf.default.accept_source_route = 0
net.ipv4.tcp_tw_recycle = 1
net.ipv4.tcp_max_syn_backlog = 4096
net.ipv4.conf.all.arp_filter = 1
net.ipv4.ip_local_port_range = 1025 65535
net.core.netdev_max_backlog = 10000
vm.overcommit_memory = 2
```

Set the following parameters in the `/etc/security/limits.conf` file:

```
soft nofile 65536
hard nofile 65536
soft nproc 131072
hard nproc 131072
```

Be sure to **reboot** after changing these kernel parameters.

### Running the Greenplum Installer

Download Greenplum for your operating system. For a free Community Edition, you
can find the download link and the official guide on the [Pivotal
website](http://pivotal.io/big-data/pivotal-greenplum).

Install Greenplum using the downloaded package:

```bash
$ unzip greenplum-db-4.2.x.x-PLATFORM.zip
$ /bin/bash greenplum-db-4.2.x.x-PLATFORM.bin
```

From now on, we assume your Greenplum are installed into
`/usr/local/greenplum-db-4.2.x.x`. If not, be aware of changes in the following
guide.

### Set Greenplum related session variables

To set Greenplum related session variables, modify your
`~/.bashrc` script and add the line:

```bash
# File: ~/.bashrc
source /usr/local/greenplum-db/greenplum_path.sh
```

### Configure ssh with localhost

Now you need to generate ssh keys for `localhost`. Run:

```bash
$ gpssh-exkeys -h localhost
```

Then you should be able to `ssh` into `localhost` without password, and you can
move on.

### Create directories for the database

Create the master and segment directories, where the database files will
be stored. **Be sure that you have write permission to these directory**.

```bash
$ mkdir ~/greenplumdb
$ mkdir ~/greenplumdb/master
$ mkdir ~/greenplumdb/data1
```

### Configure the Greenplum database in single-node mode

Copy the sample configuration files `gpinitsystem_singlenode` and
`hostlist_singlenode` to your home directory:

```bash
$ cd ~
$ cp /usr/local/greenplum-db/docs/cli_help/gpconfigs/gpinitsystem_singlenode .
$ cp /usr/local/greenplum-db/docs/cli_help/gpconfigs/hostlist_singlenode .
$ chmod 755 hostlist_singlenode gpinitsystem_singlenode
```

Open `hostlist_singlenode` and replace the existing line with `localhost`.

Open `gpinitsystem_singlenode` and make the following changes:

```bash
# MACHINE_LIST_FILE=./hostlist_singlenode
declare -a DATA_DIRECTORY=(~/greenplumdb/data1)
MASTER_HOSTNAME=localhost
MASTER_DIRECTORY=~/greenplumdb/master
```
Save and exit. Then run the following command to initialize the database:

```bash
$ gpinitsystem -c gpinitsystem_singlenode -h hostlist_singlenode
```

Your database will be created (this may take a while)

### Configure PATH to add the master data directory

Configure the `MASTER_DATA_DIRECTORY` path into your bash source:

```bash
$ echo "export MASTER_DATA_DIRECTORY=/greenplumdb/master/gpsne-1" >> ~/.bashrc
$ source ~/.bashrc
```

### Start the database server

Execute the following command to launch the database server

```bash
$ gpstart
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

Use `gpstop` and `gpstart` to stop / start the Greenplum server at any time.



## <a name="faq" href="#"></a> FAQs

- **When I use Greeplum, I see the error "ERROR: data line too long. likely due to
invalid csv data". But my program runs fine using PostgreSQL.**

        Tune the `gp_max_csv_line_length` parameter in Greenplum.

- **I get the following error: Could not reserve enough space for object heap.
  Error: Could not create the Java Virtual Machine.**

        Tune `MaxPermSize` in Java, e.g., `-XX:MaxPermSize=128m`.

        Another option, especially if you are running in a Virtual Machine
        rather than on "bare metal", is to add `vm.overcommit_memory = 1` to
        `/etc/syctl.conf` and then run `sudo sysctl -p` (Thanks to Michael Goddard
        for providing this answer).

- **How do I enable fuzzy string match / install "contrib" module in Greenplum?**

        To enable *fuzzystringmatch* / the *contrib* module available for PostgreSQL
        for Greenplum, see [this
        link](http://blog.2ndquadrant.com/fuzzystrmatch_greenplum/).

