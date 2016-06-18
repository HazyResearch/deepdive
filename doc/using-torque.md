---
layout: default
title: Using DeepDive with the Torque scheduler
---

# Introduction

In addition to exploiting parallelism by using the cores that are available locally, DeepDive supports using the Torque scheduler to parallelize tasks in the dataflow. This functionality is made possible by the concept of _compute drivers_ that are implemented in DeepDive. Compute drivers extend DeepDive's capability of running processes not only locally, but also through utilizing various compute clusters like Torque, SLURM, PBS, Hadoop, etc.

The [Torque scheduler](https://en.wikipedia.org/wiki/TORQUE) is one such compute cluster management software used by many universities and research institutions that usually require high performance computing. In Stanford, the [computing cluster in InfoLab](http://snap.stanford.edu/moin/InfolabClusterCompute) uses Torque.

A possible reason to use the Torque compute driver is that the worker nodes being managed by the scheduler have much faster processors and more memory, therefore allowing parallelizable processes associated with a certain DeepDive application to be executed much quicker. In addition, if the data set is too big such that it doesn't fit locally, then it might be useful to use a compute cluster that shares a file system.

## Prerequisites

In this article, the submission node (or the master node) is the node that controls the worker (or slave nodes), and where the scheduler commands are issued. The workstation is where the user invokes DeepDive commands, and where the DeepDive application is located.

Usage of the Torque compute driver requires a specific network topology. In particular, the following assumptions are made:

-   There exists a shared file system between the master node and the slave nodes.
-   The database associated with the DeepDive application can be accessed by the workstation. It might not necessarily be accessible by the master or slave nodes.
-   The user can perform passwordless SSH from his/her workstation to the master node, and has sufficient privileges to submit jobs.
-   The same version of DeepDive is installed in both *the workstation* and the *master node*.

## Configuration

To configure the Torque compute driver, ensure that the configuration file named `computers.conf` exists in the DeepDive application directory. The configuration file uses the same format as other DeepDive configuration files.

The following is a sample of the configuration file of a DeepDive application using the Torque scheduler.

```bash
$ cat computers.conf
deepdive.computer: infolab

deepdive.computers.local {
  type: local
}

deepdive.computers.infolab {
    type: torque

    num_processes: 3

    ssh_user: user
    ssh_host: ilhead1.stanford.edu

    remote_deepdive_app_base: "/dfs/scratch0/user/dd_app"

    # Left unspecified.
    # remote_deepdive_transient_base: ""

    poll_period_secs: 5

    # Left unspecified.
    #excludes: []
}
```

The following is an explanation of the options available under the scope `deepdive.computers.[name]`:

-   `type` (required): Controls the number of worker nodes to use.
-   `num_processes` (optional): Controls the number of worker nodes to use. Can be overwritten by setting the environment variable DEEPDIVE\_NUM\_PROCESSES. If unset, defaults to the number of nodes in the workstation. <todo>Well, this is quite awkward. Maybe for remote schedulers, should default to 1</todo>
-   `ssh_user`(required): The username to SSH to the master node.
-   `ssh_host`(required): The hostname of the master node.
-   `remote_deepdive_app_base`(required): A remote directory in the shared file system where DeepDive will copy and synchronize the application directory to. The structure of the application in the remote directory is intended to be very similar to the local directory (see Mechanism for more details).
-   `remote_deepdive_transient_base`(optional): A remote directory in the shared file system to upload transient data (input and output data, in particular). If left blank
-   `poll_period_secs`(required): The period at which DeepDive polls for statuses of jobs that are being executed in the worker nodes.
-   `excludes`(optional): Directories and files that should be excluded for synchronization.

## Mechanism

The following information is unnecessary if only using the Torque compute driver, but might be useful for developers.

An overview of how the Torque compute driver works is as follows. The driver first copies the application directory in the workstation and the input data (from the SQL query) to the specified remote directories (as the remote application and transient directories might be different) in the shared filesystem. Based on the number of processes, the input data is then split in the remote directory.

Afterwards, a template of the submission script (which contains the command to be executed by each worker node, along with other information) is generated in the submission node. This submission script is then submitted to the scheduler through the master node. Each worker node then processes a chunk of the data that was previously split, and generates the respective chunks of output.

The partial output is then combined and downloaded back to the database accessible by the workstation. Note that transient data (i.e. input and output data) is streamed to the remote directory, and hence will not take up space in the workstation during loading and unloading of data.

Since the driver relies on invocations of `deepdive compute remote-*`, that are relevant only when executed in the master node, it is required that the same version of DeepDive be installed in the master node and workstation. Also, the remote directory structure which contains the application directory is intended to look very similar to the local application directory structure. The idea is so that even when running remote commands, the feeling of running the application locally is preserved. In addition to making this simpler for the user, it also reduces the complexity of implementation.

Although the remote and local directory structures are similar, there is one exception. The application name in the remote directory is a md5 hash of the local application name (i.e. `basename $DEEPDIVE_APP`) and the local user. This allows separate applications that might have the same name but different local directories to use the same `computers.conf` file.

## Notes

Note the following limitations that the current implementation of the Torque driver has:

-   Currently, the Torque driver doesn't support using multiple CPUs in the scheduler.
