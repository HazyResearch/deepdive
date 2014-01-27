---
layout: default
---

# Using GreenPlum with DeepDive

This documentation provides a simple installation guide for [GreenPlum](http://www.gopivotal.com/products/pivotal-greenplum-database) to work with DeepDive. We will use the single-node mode of GreenPlum for demonstration purposes. It should work identically with the multi-node configuration.

**After installing GreenPlum, DeepDive should work well with it. The rest steps are identical with the documentation for [PostgreSQL](postgresql.html)**.


### Installing GreenPlum on Mac OS X

We provide an installation guide for Mac OS X. Other operating systems should go through a similar process.

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


Be sure to **restart your Mac** after changing kernel parameters.

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

### Configure

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


### Greenplum FAQ

**When I use Greeplum, I see the error "ERROR: data line too long. likely due to invalid csv data". But my program runs fine using PostgreSQL.**

Tune `gp_max_csv_line_length` in Greenplum.

### References

References: [http://dwarehouse.wordpress.com/2012/06/05/installing-greenplum-database-community-edition-on-a-mac-os-x-10-7/](http://dwarehouse.wordpress.com/2012/06/05/installing-greenplum-database-community-edition-on-a-mac-os-x-10-7)

