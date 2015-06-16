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

You can install DeepDive and all its dependencies with a single command and verify.

1. Open your terminal and run this:
   <pre style="width:80%; margin:0 auto; padding:20px;"><code><big style="font-size:175%;">bash <(curl -fsSL deepdive.stanford.edu/install)</big></code></pre>

2. Select `deepdive` when asked.

    ```
    $ bash <(curl -fsSL deepdive.stanford.edu/install)
    ### DeepDive installer for Mac
    1) deepdive               3) deepdive_git_repo      5) postgres
    2) deepdive_build_deps    4) deepdive_runtime_deps
    # Select what to install (enter a number or q to quit)? 1
    [...]
    ```

3. You'll see that all dependencies are being installed. Note that some steps may ask your password.

4. DeepDive source tree is cloned at `./deepdive`, and some executables are installed under `~/local/`.

5. Since DeepDive needs a database installation to run correctly.  You should install one of the provided options:
    * postgres
    * postgres_xl
    * greenplum
    * mysql

6. Finally, you may run tests in DeepDive's source tree to make sure everything will run fine.

    ```bash
    cd ./deepdive
    make test
    ```

Congratulations! DeepDive is now installed on your system, and you can proceed to the [next steps](walkthrough/walkthrough.html).



## Installing from Source

If you need to install DeepDive on a different Linux distribution, the following steps can be followed.

### <a name="dependencies" href="#"></a> Install Dependencies

The following software packages must be installed to run DeepDive:

- [Git](http://git-scm.com/book/en/Getting-Started-Installing-Git) needed to get DeepDive's source tree.
- [Java](http://www.oracle.com/technetwork/java/javase/downloads/jre7-downloads-1880261.html)
  (version **1.7.0_45 or higher**)
- [Python](https://www.python.org/) *2.X* (not Python 3.X)
- [Gnuplot](http://www.gnuplot.info/)
- A database installation
    - [Postgres](http://wiki.postgresql.org/wiki/Detailed_installation_guides)
    - [Postgres-XL](../advanced/pgxl.html)
    - [Greenplum](../advanced/greenplum.html)
    - [MySQL](../advanced/mysql.html)

### Downloading Source

Clone the Deepdive repository in the desired directory with:

```bash
git clone https://github.com/HazyResearch/deepdive.git
```

This will create the directory `deepdive`. In the rest of the document, all
paths are relative to this directory. We denote the *full* path to this
directory as `DEEPDIVE_HOME`.


### Building Source

We provide a Makefile to quickly install DeepDive. Under `DEEPDIVE_HOME`, run the following command to install DeepDive:

```bash
make
```

This will first extract our sampler library, compile and pack DeepDive, and deploy the executable to `$HOME/local/bin/deepdive`. You can also find the compiled executable in `$DEEPDIVE_HOME/target/pack/bin/deepdive`.

After `make` you should be already able to use DeepDive on your machine. You can either use the command `deepdive` (make sure to add `$HOME/local/bin/` into your `$PATH`) to run the compiled binary, or compile and run each time by `sbt run` under `DEEPDIVE_HOME`.

After that, make sure to **set environmental variables** needed for running sampler, depending on what OS you are using:

On Mac:

```bash
export DEEPDIVE_HOME=[your path to install deepdive]
```

On Linux:

```bash
export DEEPDIVE_HOME=[your path to install deepdive]
```

(The set of environmental variable `DEEPDIVE_HOME` is optional. If not set, DeepDive will assume that `DEEPDIVE_HOME` is current directory.)

Finally, to ensure DeepDive works on your machine, then test DeepDive functionality by running all the script:

```bash
make test
```

More explanation of what the Makefile does are in below sections.

#### <a name="sampler" href="#"></a> Installing the DimmWitted Sampler

DimmWitted is our fast sampler that DeepDive use in the sampling step.
DimmWitted binaries for both Mac OS X and Linux ship with DeepDive in the
`util/` directory. These work out of the box on many modern systems, but setting
up dependencies may be required on others.

*Note:* DimmWitted only supports 64-bit systems.

We ship pre-built dependencies for Linux and Mac systems in the `lib/` folder, and the first step of the Makefile is to extract them depending on the OS.

After this step, be sure to set environmental variables as described above, to make sure the extracted directories are included in the appropriate search paths.

#### Packaging DeepDive

The Makefile then compiles DeepDive by `sbt pack`. It involves downloading all the necessary frameworks and libraries used
internally, and may take some time. If the operation completes with success, the
last line of the `sbt pack` output should read something similar to:

    [success] Total time: 110 s, completed Jan 28, 2014 6:31:09 PM

If the setup fails, check the console log for detailed error information, and make sure you have all the [dependencies](#dependencies) installed.

#### Deploying DeepDive Binary

The Makefile will finally pack the compiled code into a binary file, and deploy it into `$DEEPDIVE_HOME/target/pack/bin/deepdive`. Make sure you have write permissions to that folder. If deploying succeeded, you will see information like this:

    SUCCESS! DeepDive binary has been put into $HOME/local/bin.

### Running Tests

The command `make test` executed from the `DEEPDIVE_HOME` directory runs the
sanity checks to make sure DeepDive runs properly on your machine. DeepDive will create a database `deepdive_test` using your login username and no password, and connect to the database during the tests. You should make sure Postgres is successfully installed to pass the tests.

In case of success, the last lines of `make test` output is something similar to:

    [info] All tests passed.
    [success] Total time: 35 s, completed Jan 29, 2014 9:59:57 AM

Congratulations, DeepDive is now running on your system!

Note that for the system to work properly, the `deepdive` folder must *not* be
renamed.

### <a id="ddlib" href="#"></a> Additional step: setting up environment variables for DDLib

DDLib is our Python library that provides useful utilities such as "Span" to
manipulate elements in sentences. It can be useful to write your UDFs during the
[extraction step](overview.html#extraction). To use `ddlib`,
`$DEEPDIVE_HOME/ddlib` must be added to the `PATH` or `PYTHONPATH` environment
variables:

```bash
export PYTHONPATH=$DEEPDIVE_HOME/ddlib:$PYTHONPATH
```

For more documentation about `ddlib`, please refer to its pydoc. Specifically, in a python terminal:

```python
>>> import ddlib
>>> help(ddlib.dd)
```

You will be able to see the pydoc to all its modules.

