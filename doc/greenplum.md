---
layout: default
---

# Using GreenPlum with DeepDive

This documentation provides a simple installation guide for [GreenPlum](http://www.gopivotal.com/products/pivotal-greenplum-database), to work with DeepDive. We will use single-node mode of GreenPlum.


## Installation with Mac OS X

We provide an installation guide with Mac OS X. Other operating systems should go through a similar process.


### Installing GreenPlum

Download GreenPlum for your operating system. For a free Community Edition, find a download link as well as an official guide at [http://www.gopivotal.com/products/pivotal-greenplum-database](http://www.gopivotal.com/products/pivotal-greenplum-database). 

Install GreenPlum using the downloaded package.  From now on, we assume your Greenplum are installed into `/usr/local/greenplum-db-x.x.x.x`. If it is not installed into `/usr/local/`, be sure to add changes to following.

### Set Greenplum related session variables

Run the script in the GreenPlum home folder: 
`sh /usr/local/greenplum-db-x.x.x.x/greenplum_path.sh`

This will create a symbolic link in `/usr/local/greenplum-db/`.

To set Greenplum related session variables: add into your bash source `/usr/local/greenplum-db/greenplum_path.sh`. Specifically, modify your `~/.bash_profile` and add a line:

`$ vim ~/.bash_profile`

    source /usr/local/greenplum-db/greenplum_path.sh


### Configure Host settings

Open a terminal window and connect to root and modify /etc/sysctl.conf file.

`$ sudo vim /etc/sysctl.conf`

    kern.sysv.shmmax=2147483648
    kern.sysv.shmmin=1
    kern.sysv.shmmni=64
    kern.sysv.shmseg=16
    kern.sysv.shmall=524288
    kern.maxfiles=65535
    kern.maxfilesperproc=65535
    net.inet.tcp.msl=60


Save and exit the file.

Add the following line in /etc/hostconfig

`$ vi /etc/hostconfig`

    HOSTNAME=localhost

Save and exit the file.

Be sure to **restart your Mac** now.

### Configure ssh with localhost

Now you need to generate ssh keys for localhost.

Be sure that you are able to ssh into localhost without password.

running `$ gpssh-exkeys -h localhost`. 

If it fails, try to first be able to ssh into localhost with password, then follow these steps:
1. `ssh-keygen -t rsa` (Press enter for each line)
2. `cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys`
3. `chmod og-wx ~/.ssh/authorized_keys`



### Create folders for database

Create master and segment folders. This is where the database files will
be stored. Be sure that you have write permission to these folders.

`$ mkdir /greenplumdb`
`$ mkdir /greenplumdb/master`
`$ mkdir /greenplumdb/data1`

### Configure Greenplum database on single-node mode

Copy sample configuration files `$ gpinitsystem_singlenode` and `$ hostlist_singlenode` to your working directory.

Assuming your working directory is `~`.

`$ cd ~`
`$ cp /usr/local/greenplum-db/docs/cli_help/gpconfigs/gpinitsystem_singlenode .`
`$ cp /usr/local/greenplum-db/docs/cli_help/gpconfigs/hostlist_singlenode .`
`chmod 755 hostlist_singlenode gpinitsystem_singlenode`

Open `hostlist_singlenode` and replace the existing line with `localhost`:

`$ vi hostlist_singlenode`

    localhost

Open `gpinitsystem_singlenode` and make the following changes:

    # MACHINE_LIST_FILE=./hostlist_singlenode

    declare -a DATA_DIRECTORY=(/greenplumdb/data1)

    MASTER_HOSTNAME=localhost

    MASTER_DIRECTORY=/greenplumdb/master
        
Save and exit. Then run the following command to create the database:

`$ gpinitsystem -c gpinitsystem_singlenode -h hostlist_singlenode`

After a while, your database server is created.

### Configure

Add `export MASTER_DATA_DIRECTORY=/greenplumdb/master/gpsne-1` into your bash source `~/.bash_profile`:

`$ echo "export MASTER_DATA_DIRECTORY=/greenplumdb/master/gpsne-1" >> ~/.bash_profile`

### Stop and Start the database server

You may use `gpstop` and `gpstart` to stop / start the Greenplum server.

### Verify the installation

Follow the links and you should get similar output.

`$ psql postgres`

    psql (8.2.15)
    Type “help” for help.

    postgres=#

`postgres=# \l`

                    List of databases
       Name    | Owner | Encoding | Access privileges
    -----------+-------+----------+-------------------
     postgres  | Xxx   | UTF8     |
     template0 | Xxx   | UTF8     | =c/Xxx  
                                  : Xxx=CTc/Xxx  
     template1 | Xxx   | UTF8     | =c/Xxx  
                                  : Xxx=CTc/Xxx  
    (3 rows)

`postgres=# \q`


### References

References: [http://dwarehouse.wordpress.com/2012/06/05/installing-greenplum-database-community-edition-on-a-mac-os-x-10-7/](http://dwarehouse.wordpress.com/2012/06/05/installing-greenplum-database-community-edition-on-a-mac-os-x-10-7/)



----

## Configuring DeepDive to work with GreenPlum

After installing GreenPlum, DeepDive should work well with it. The rest steps are identical with the documentation for [PostgreSQL](postgresql.md).
