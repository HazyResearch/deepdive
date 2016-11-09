---
layout: default
title: Installation Guide
no_toc: true
---

# DeepDive installation guide

DeepDive runs on macOS, Linux, and in Docker containers.
This document explains how to install or launch DeepDive on your system.

## Launch without installing

We provide Docker images and Docker Compose configuration to just launch and use DeepDive.

1. Make sure you have [Docker Compose](https://docs.docker.com/compose/install/) installed.
    (If Docker does not work for you, then try quick installation below.
    If you lack system administration rights, then try the `deepdive_from_release` option.)

2. Open your terminal and run this:
   <pre style="width:80%; margin:0 auto; padding:20px;"><code><big style="font-size:175%;">bash <(curl -fsSL git.io/getdeepdive)</big></code></pre>

3. Select `deepdive_docker_sandbox` when asked.

    ```
    ### DeepDive installer for Mac
    1) deepdive                   5) jupyter_notebook
    2) deepdive_docker_sandbox    6) postgres
    3) deepdive_example_notebook  7) run_deepdive_tests
    4) deepdive_from_release      8) spouse_example
    # Install what (enter to repeat options, a to see all, q to quit, or a number)? 2
    [...]
    ```

4. It launches a [DeepDive container](https://hub.docker.com/r/hazyresearch/deepdive/) along with a Postgres database container we tweaked for DeepDive.

5. Point your web browser to the [tutorial notebook](http://0.0.0.0:8888/notebooks/deepdive-examples/spouse/DeepDive%20Tutorial%20-%20Extracting%20mentions%20of%20spouses%20from%20the%20news.ipynb) or a [terminal](http://0.0.0.0:8888/terminals/1).


### Data Persistency

Here are a few IMPORTANT things to keep in mind about data:

* All data in the database is kept under `deepdive-*/sandbox/database/` on the host, so you can find it on subsequent launches.

* To exchange files between host and container, use the `deepdive-*/sandbox/workdir/` on host, which is mounted on `/ConfinedWater/workdir/` in the container.

* WARNING!!! Changes to everything else in the container, including files under `deepdive-examples/`, disappear when you shut down the Docker Compose, e.g., with `docker-compose down`.



## Quick installation

We provide a simple installation method for the following supported systems:

* GNU/Linux: Debian (7, 8, or later) and Ubuntu (12.04LTS, 14.04LTS, 16.04LTS, or later)
    * [Docker](https://docker.io)
    * [VirtualBox](https://help.ubuntu.com/community/VirtualBox)
    * [AWS EC2](using-ec2.md)
* macOS with [Homebrew](http://brew.sh)

You can install DeepDive and all its dependencies with a single command.

1. Open your terminal and run this:
   <pre style="width:80%; margin:0 auto; padding:20px;"><code><big style="font-size:175%;">bash <(curl -fsSL git.io/getdeepdive)</big></code></pre>

2. Select `deepdive` or `deepdive_from_release` when asked.
    Choose the latter option if you simply want to install DeepDive without any of its runtime dependencies.

    ```
    ### DeepDive installer for Mac
    1) deepdive                   5) jupyter_notebook
    2) deepdive_docker_sandbox    6) postgres
    3) deepdive_example_notebook  7) run_deepdive_tests
    4) deepdive_from_release      8) spouse_example
    # Install what (enter to repeat options, a to see all, q to quit, or a number)? 1
    [...]
    ```

    Here are some more details of each options:
    * For installation with the `deepdive` option, All runtime dependencies are installed.
    Note that some steps may ask your password.
    * If you don't have permission to install the dependencies, you may want to use the `deepdive_from_release` option and ask the system administrator to install DeepDive's runtime dependencies with the following command.

        ```bash
        bash <(curl -fsSL git.io/getdeepdive) _deepdive_runtime_deps
        ```

    * For installation with `deepdive_from_source` option, extra build dependencies are installed, and DeepDive source tree is cloned at `./deepdive`, then executables are installed under `~/local/bin/`.
    You can run tests in DeepDive's source tree to make sure everything will run fine.
    See the [developer's guide](developer.md#build-test) for more details.

        ```bash
        cd ./deepdive
        make test
        ```

3. To use the `deepdive` command on a regular basis, it is recommended to add the following line to your `~/.bash_profile`.  Otherwise, you need to always type its full path: `~/local/bin/deepdive`.

    ```
    export PATH=~/local/bin:"$PATH"
    ```

4. Since DeepDive needs a database installation to run correctly.  You should use one of the provided installer options:
    * [`postgres`](http://wiki.postgresql.org/wiki/Detailed_installation_guides)
        DeepDive works with most of the recent versions of PostgreSQL.
        However, 9.3+ is recommended to use all functionality.
    * [`postgres_xl`](using-pgxl.md)
        DeepDive works with current release of PostgreSQL-XL, which is based on PostgreSQL 9.2.
        PL/Python extension is required.
    * [`greenplum`](using-greenplum.md)
        DeepDive works with recent releases of Greenplum, which is based on PostgreSQL 8 that may lack some features that are required by some advanced DeepDive functionality.
        PL/Python extension is required.
    * `mysql`
        DeepDive provides minimal support for MySQL and MySQL Cluster, but PostgreSQL-based databases are strongly recommended.


5. You can verify whether your installation is correct using the `run_deepdive_tests` option in the installer.
    It downloads all examples and tests for the DeepDive release and runs the tests using the installed one.
    To only download the example applications, use the `deepdive_examples_tests` option.

Congratulations! DeepDive is now installed on your system, and you can proceed to the [next steps](example-spouse.md).



## Installing from source

Using the quick installation method is recommended unless you want to use a development branch or modify DeepDive engine itself.
If you still want to build DeepDive from source code yourself, follow the [developer's guide](developer.md#build-test).
