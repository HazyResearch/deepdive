---
layout: default
---

# Using GreenPlum with DeepDive

This documentation provides a simple installation guide for [GreenPlum](http://www.gopivotal.com/products/pivotal-greenplum-database) to work with DeepDive. We will use the single-node mode of GreenPlum for demonstration purposes. It should work identically with the multi-node configuration.

**After installing GreenPlum, DeepDive should work well with it. The rest steps are identical with the documentation for [PostgreSQL](postgresql.html)**.


## Installation with Centos Linux

The following guide sets up GreenPlum on Centos 5.6 x64.

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

*NOTE: For RHEL version 6.x platforms, the above parameters do not include the `sysctl.` prefix, as follows:*

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

<!-- TODO:zifei  Errors here...

In the file `/etc/hosts` comment out the line beginning with `::1`, as it could confuse the database when it resolves the hostname for localhost. 

    $ vi /etc/hosts

    # Comment the line starting with ::1
    # ::1         localhost localhost.localdomain localhost6 localhost6.localdomain6  
 -->


Be sure to **reboot** after changing kernel parameters.

    $ sudo reboot

### Running the GreenPlum Installer

Download GreenPlum for your operating system. For a free Community Edition, find a download link as well as an official guide at [http://www.gopivotal.com/products/pivotal-greenplum-database](http://www.gopivotal.com/products/pivotal-greenplum-database), or directly download [here](http://downloads.cfapps.io/gpdb_db_el5_64). 

Install GreenPlum using the downloaded package: 

    $ unzip greenplum-db-4.2.x.x-PLATFORM.zip
    $ /bin/bash greenplum-db-4.2.x.x-PLATFORM.bin

From now on, we assume your Greenplum are installed into `/usr/local/greenplum-db-x.x.x.x`. If not, be aware of changes in the following guide.

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

Start the database:

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




----


## Installation with Mac OS X

### Installing GreenPlum

1. Download GreenPlum for your operating system. For a free Community Edition, you can find a download link as well as an official guide at [http://www.gopivotal.com/products/pivotal-greenplum-database](http://www.gopivotal.com/products/pivotal-greenplum-database). 


2. Install GreenPlum using the downloaded package.  From now on, we assume your Greenplum is installed into `/usr/local/greenplum-db-x.x.x.x`. If not, be aware of changes in the following guide.

### Set Greenplum related session variables

Run the script in the GreenPlum home folder: 

    $ sh /usr/local/greenplum-db-x.x.x.x/greenplum_path.sh

This will create a symbolic link in `/usr/local/greenplum-db/`.

To set Greenplum related session variables: add into your bash source `/usr/local/greenplum-db/greenplum_path.sh`. Specifically, modify your `~/.bash_profile` and add a line:

    $ vi ~/.bash_profile

    source /usr/local/greenplum-db/greenplum_path.sh


### Configure Host settings

Open a terminal window and connect to root and modify /etc/sysctl.conf file (add following lines).

    $ sudo vi /etc/sysctl.conf

    kern.sysv.shmmax=2147483648
    kern.sysv.shmmin=1
    kern.sysv.shmmni=64
    kern.sysv.shmseg=16
    kern.sysv.shmall=524288
    kern.maxfiles=65535
    kern.maxfilesperproc=65535
    net.inet.tcp.msl=60

Add the line `HOSTNAME=localhost` in `/etc/hostconfig`:

    $ vi /etc/hostconfig

    HOSTNAME=localhost


Be sure to **reboot your Mac** after changing kernel parameters.

### Configure ssh with localhost

If you have not done so previousy, you akso need to generate ssh keys for your localhost.

Be sure that you are able to ssh into localhost without password. Try running `$ gpssh-exkeys -h localhost`. If it fails, try to first be able to ssh into localhost with password, then follow these steps:

1. `$ ssh-keygen -t rsa` (Press enter for each line)
2. `$ cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys`
3. `$ chmod og-wx ~/.ssh/authorized_keys`

After you are able to `ssh` into `localhost` without a password you can move on.

### Create database folders

Create master and segment folders. This is where the database files will
be stored. Make sure that you have write permission to these folders.

    $ mkdir /greenplumdb
    $ mkdir /greenplumdb/master
    $ mkdir /greenplumdb/data1


### Configure Greenplum database on single-node mode

Copy sample configuration files `$ gpinitsystem_singlenode` and `$ hostlist_singlenode` to your working directory.

Assuming your working directory is `~`:

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
    declare -a DATA_DIRECTORY=(/greenplumdb/data1)
    MASTER_HOSTNAME=localhost
    MASTER_DIRECTORY=/greenplumdb/master
        
Save and exit. Then run the following command to create the database:

    $ gpinitsystem -c gpinitsystem_singlenode -h hostlist_singlenode

After a while, your database server is created.

### Configure PATH to add master data directory

Configure the `MASTER_DATA_DIRECTORY` path into your bash source:

    $ echo "export MASTER_DATA_DIRECTORY=/greenplumdb/master/gpsne-1" >> ~/.bash_profile


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



----

## Configuring DeepDive to work with GreenPlum

After installing GreenPlum, DeepDive should work well with it. Most of the rest steps are identical with the documentation for [PostgreSQL](postgresql.html). 

<!-- However, there are some specific problems to notice in GreenPlum:

GreenPlum Specific Problems:
1. fuzzy string match
2. TODO

-->



## Greenplum FAQ

**When I use Greeplum, I see the error "ERROR: data line too long. likely due to invalid csv data". But my program runs fine using PostgreSQL.**

Tune `gp_max_csv_line_length` in Greenplum.


**Could not reserve enough space for object heap. Error: Could not create the Java Virtual Machine.**

Tune `MaxPermSize` in Java. e.g. `-XX:MaxPermSize=128m`.