---
layout: default
---

# DeepDive Installation Guide 

This document explains how to install DeepDive.

### <a name="dependencies" href="#"></a> Dependencies

DeepDive uses PostgreSQL, Scala, and Python (version 2.X). The following
software packages must be installed to run DeepDive:

- [Java](http://www.oracle.com/technetwork/java/javase/downloads/jre7-downloads-1880261.html)
  (version **1.7.0_45 or higher**)

- [Python](https://www.python.org/) *2.X* (not Python 3.X)
- [PostgreSQL](http://wiki.postgresql.org/wiki/Detailed_installation_guides)
- [SBT](http://www.scala-sbt.org/release/tutorial/Setup.html)
- [Gnuplot](http://www.gnuplot.info/)

[Git](http://git-scm.com/book/en/Getting-Started-Installing-Git) is needed to
install DeepDive. 

### Download 

Clone the Deepdive repository in the desired directory with:

```bash
git clone https://github.com/HazyResearch/deepdive.git
```

This will create the directory `deepdive`. In the rest of the document, all
paths are relative to this directory. We denote the *full* path to this
directory as `DEEPDIVE_HOME`.


### Quick install

<!-- TODO (All): needs another one to go through the install process to make sure it works. -Zifei -->

We provide a Makefile to quickly install DeepDive. Under `DEEPDIVE_HOME`, run the following command to install DeepDive:

```bash
make
```

This will first extract our sampler library, compile and pack DeepDive, and deploy the executable to `${HOME}/local/bin/deepdive`. You can also find the compiled executable in `DEEPDIVE_HOME/target/pack/bin/deepdive`.

After `make` you should be already able to use DeepDive on your machine. You can either use the command `deepdive` (make sure to add `${HOME}/local/bin/` into your `$PATH`) to run the compiled binary, or compile and run each time by `sbt run` under `DEEPDIVE_HOME`.

After that, make sure to **set environmental variables** needed for running sampler, depending on what OS you are using:

On Mac:
  
```bash
export DEEPDIVE_HOME=[your path to install deepdive]
export LD_LIBRARY_PATH=$DEEPDIVE_HOME/lib/dw_mac/lib/protobuf/lib:$DEEPDIVE_HOME/lib/dw_mac/lib
export DYLD_LIBRARY_PATH=$DEEPDIVE_HOME/lib/dw_mac
```

On Linux:
  
```bash
export DEEPDIVE_HOME=[your path to install deepdive]
export LD_LIBRARY_PATH=$DEEPDIVE_HOME/lib/dw_linux/lib:$DEEPDIVE_HOME/lib/dw_linux/lib64
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

#### Compiling DeepDive

The Makefile then compiles DeepDive by `sbt pack`. It involves downloading all the necessary frameworks and libraries used
internally, and may take some time. If the operation completes with success, the
last line of the `sbt pack` output should read something similar to: 

    [success] Total time: 110 s, completed Jan 28, 2014 6:31:09 PM

If the setup fails, check the console log for detailed error information, and make sure you have all the [dependencies](#dependencies) installed.

#### Deploying DeepDive Binary

The Makefile will finally pack the compiled code into a binary file, and deploy it into `DEEPDIVE_HOME/target/pack/bin/deepdive`. Make sure you have write permissions to that folder. If deploying succeeded, you will see information like this:

    SUCCESS! DeepDive binary has been put into $HOME/local/bin.

#### Running Tests

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
`DEEPDIVE_HOME/ddlib` must be added to the `PATH` or `PYTHONPATH` environment
variables:

```bash
export PYTHONPATH=DEEPDIVE_HOME/ddlib:$PYTHONPATH
```

For more documentation about `ddlib`, please refer to its pydoc. Specifically, in a python terminal:

```python
>>> import ddlib
>>> help(ddlib.dd)
```

You will be able to see the pydoc to all its modules.

