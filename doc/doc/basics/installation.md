---
layout: default
---

# DeepDive Installation Guide

DeepDive runs on Linux or Mac OS X.
This document explains how to install DeepDive on your system.

## Quick Installation

We provide a simple installation method for the following supported systems:

* Ubuntu Linux (12.04LTS, 14.04LTS, or later)
    * [VirtualBox](https://help.ubuntu.com/community/VirtualBox)
    * [AWS EC2](../advanced/ec2.html)
    * [Docker](../advanced/docker.html)
* Mac OS X with [Homebrew](http://brew.sh)

You can install DeepDive and all its dependencies with a single command.

1. Open your terminal and run this:
   <pre style="width:80%; margin:0 auto; padding:20px;"><code><big style="font-size:175%;">bash <(curl -fsSL deepdive.stanford.edu/install)</big></code></pre>

2. Select `deepdive` or `deepdive_from_release` when asked.
    Choose the latter option if you simply want to install DeepDive without any of its runtime dependencies.

    ```
    $ bash <(curl -fsSL deepdive.stanford.edu/install)
    ### DeepDive installer for Mac
    1) deepdive               3) deepdive_from_source
    2) deepdive_from_release  4) postgres
    # Select what to install (enter a number or q to quit)? 1
    [...]
    ```

    Here are some more details of each options:
    * For installation with the `deepdive` option, All runtime dependencies are installed.
    Note that some steps may ask your password.
    * If you don't have permission to install the dependencies, you may want to use the `deepdive_from_release` option and ask the system administrator to install DeepDive's runtime dependencies with the following command.

        ```bash
        bash <(curl -fsSL deepdive.stanford.edu/install) _deepdive_runtime_deps
        ```
    * For installation with `deepdive_from_source` option, extra build dependencies are installed, and DeepDive source tree is cloned at `./deepdive`, then executables are installed under `~/local/bin/`.
    You can run tests in DeepDive's source tree to make sure everything will run fine.
    See the [developer's guide](../advanced/developer.html#build-test) for more details.

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
    * [`postgres_xl`](../advanced/pgxl.html)
    * [`greenplum`](../advanced/greenplum.html)
    * [`mysql`](../advanced/mysql.html)

Congratulations! DeepDive is now installed on your system, and you can proceed to the [next steps](walkthrough/walkthrough.html).

