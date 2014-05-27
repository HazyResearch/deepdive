---
layout: default
---

# Using Greenplum with DeepDive

This documentation provides a simple installation guide for [Greenplum](http://www.gopivotal.com/products/pivotal-greenplum-database) to work with DeepDive. We will use the single-node mode of Greenplum for demonstration purposes. It should work identically with the multi-node configuration.

**After installing GreenPlum, DeepDive should work well with it. The rest steps are identical with the documentation for [PostgreSQL](postgresql.html)**. There are a few caveats though, in the following section:

## Important notes for GreenPlum users

- Be sure to refer to [GreenPlum manuals](http://media.gpadmin.me/wp-content/uploads/2012/11/GPDBAGuide.pdf).

- Users should add `DISTRIBUTED BY` clause in all `CREATE TABLE` commands. **Do not use variable id** as distribution key. **Do not use** distribution key that is **not initially assigned**.


## Note

If you are installing Greenplum on a machine without root access, skip the step **Setting the Greenplum Recommended OS Parameters**, and if you cannot change your settings to login to localhost without a password, skip the step **Configure ssh with localhost**.

Note that you will be prompted to enter your password to `localhost` several times throughout the installation process.

If you do not have write permissions to the /usr/local/ directory: let GREENPLUM_DIR be the directory where you wish to install Greenplum. In the steps below, replace /usr/local/ with GREENPLUM_DIR.

In the steps **Set Greenplum related session variables** and **Configure PATH to add master data directory**, if you do not have write permissions to the ~/.bashrc file, simply run the commands `source GREENPLUM_DIR/greenplum-db/greenplum_path.sh` and `export MASTER_DATA_DIRECTORY=GREENPLUM_DIR/greenplumdb/master/gpsne-1`. In order to have these commands run every time you log in to the system, save a new script called greenplum_startup.sh with the following contents:

    source GREENPLUM_DIR/greenplum-db/greenplum_path.sh
    export MASTER_DATA_DIRECTORY=GREENPLUM_DIR/greenplumdb/master/gpsne-1

and edit your terminal settings so that this script gets executed every time the shell opens.

## Installation with Linux

The following guide sets up Greenplum on CentOS 5.6 x64 with a single-node mode.

### Setting the Greenplum Recommended OS Parameters

Set the following parameters in the `/etc/sysctl.conf` file and reboot: 

    $ sudo vi /etc/sysctl.conf

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

*NOTE:* For RHEL version 6.x platforms, the above parameters do not include the `sysctl.` prefix, as follows:

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

Set the following parameters in the `/etc/security/limits.conf` file: 

    $ sudo vi /etc/security/limits.conf

    * soft nofile 65536
    * hard nofile 65536
    * soft nproc 131072
    * hard nproc 131072

Be sure to **reboot** after changing kernel parameters.

    $ sudo reboot

### Running the Greenplum Installer

Download Greenplum for your operating system. For a free Community Edition, find a download link as well as an official guide at [http://www.gopivotal.com/products/pivotal-greenplum-database](http://www.gopivotal.com/products/pivotal-greenplum-database), or directly download [here](http://downloads.cfapps.io/gpdb_db_el5_64). 

Install Greenplum using the downloaded package: 

    $ unzip greenplum-db-4.2.x.x-PLATFORM.zip
    $ /bin/bash greenplum-db-4.2.x.x-PLATFORM.bin

From now on, we assume your Greenplum are installed into `/usr/local/greenplum-db-4.2.x.x`. If not, be aware of changes in the following guide.

### Set Greenplum related session variables

To set Greenplum related session variables: add into your bash source `/usr/local/greenplum-db/greenplum_path.sh`. Specifically, modify your `~/.bashrc` and add a line:

    $ vi ~/.bashrc

    source /usr/local/greenplum-db/greenplum_path.sh

### Configure ssh with localhost

Now you need to generate ssh keys for localhost. Run: 

    $ gpssh-exkeys -h localhost

Then you should be able to `ssh` into `localhost` without password, and you can move on.



### Create folders for database

Create master and segment folders. This is where the database files will
be stored. **Be sure that you have write permission to these folders**.


    $ mkdir ~/greenplumdb
    $ mkdir ~/greenplumdb/master
    $ mkdir ~/greenplumdb/data1

### Configure Greenplum database on single-node mode

Copy sample configuration files `$ gpinitsystem_singlenode` and `$ hostlist_singlenode` to your working directory.

Assume your working directory is `~`.


    $ cd ~
    $ cp /usr/local/greenplum-db/docs/cli_help/gpconfigs/gpinitsystem_singlenode .
    $ cp /usr/local/greenplum-db/docs/cli_help/gpconfigs/hostlist_singlenode .
    $ chmod 755 hostlist_singlenode gpinitsystem_singlenode

Open `hostlist_singlenode` and replace the existing line with `localhost`:

    $ vi hostlist_singlenode

    localhost

Open `gpinitsystem_singlenode` and make the following changes:

    $ vi gpinitsystem_singlenode

    # MACHINE_LIST_FILE=./hostlist_singlenode
    declare -a DATA_DIRECTORY=(~/greenplumdb/data1)
    MASTER_HOSTNAME=localhost
    MASTER_DIRECTORY=~/greenplumdb/master
        
Save and exit. Then run the following command to create the database:

    $ gpinitsystem -c gpinitsystem_singlenode -h hostlist_singlenode

After a while, your database server is created.

### Configure PATH to add master data directory

Configure the `MASTER_DATA_DIRECTORY` path into your bash source:

    $ echo "export MASTER_DATA_DIRECTORY=/greenplumdb/master/gpsne-1" >> ~/.bashrc

    $ source ~/.bashrc

### Start the database:

    $ gpstart


### Verify the installation

Follow the links and you should get similar output.

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

### Stop and Start the database server

You may use `gpstop` and `gpstart` to stop / start the Greenplum server at any time.



## Greenplum FAQ

**When I use Greeplum, I see the error "ERROR: data line too long. likely due to invalid csv data". But my program runs fine using PostgreSQL.**

Tune `gp_max_csv_line_length` in Greenplum.


**Could not reserve enough space for object heap. Error: Could not create the Java Virtual Machine.**

Tune `MaxPermSize` in Java, e.g., `-XX:MaxPermSize=128m`.

**How to enable fuzzy string match / install "contrib" module in GreenPlum?**

To enable *fuzzystringmatch* / the *contrib* module available for PostgreSQL for Greenplum, please check out [this link](http://blog.2ndquadrant.com/fuzzystrmatch_greenplum/).
