---
layout: default
---

# DeepDive Installation Guide 

This document explains how to install DeepDive.

### Dependencies

DeepDive uses PostgreSQL, Scala, and Python (version 2.X). The following
software packages must be installed:

- [Our DimmWitted Sampler](sampler.html)
- [Java](http://www.oracle.com/technetwork/java/javase/downloads/jre7-downloads-1880261.html)
  (version **1.7.0_45 or higher**)
- [Python](http://www.python.org/getit/) 2.X (not Python 3.X)
- [PostgreSQL](http://wiki.postgresql.org/wiki/Detailed_installation_guides)
- [SBT](http://www.scala-sbt.org/release/docs/Getting-Started/Setup.html)
- [Gnuplot](http://www.gnuplot.info/)

[Git](http://git-scm.com/book/en/Getting-Started-Installing-Git) is needed to
install DeepDive. 

### Download 

Clone the Deepdive repository in the desired directory with:     
    >> git clone https://github.com/HazyResearch/deepdive.git
This will create the directory 'deepdive'. In the rest of the document, all
paths are relative to this directory. We denote the *full* path to this
directory as `DEEPDIVE_HOME`.

### <a name="sampler" href="#"></a> Install the DimmWitted Sampler
DimmWitted is our fast sampler that is using in the sampling step.
DimmWitted binaries for both Mac OS X and Linux ship with DeepDive in the
`util/` directory. These work out of the box on many modern systems, but setting
up dependencies may be required on others.

*Note:* DimmWitted only supports 64-bit systems. 

We ship pre-built dependencies for Linux and Mac systems in the `lib/` folder,
but you need to extract them:

```bash
    cd lib
    unzip dw_mac.zip
```

The extracted directories must be included in the appropriate search paths by
setting the following environmental variables. On Mac:
  
```bash
    export LD_LIBRARY_PATH=[DEEPDIVE_HOME]/lib/dw_mac/lib/protobuf/lib:[DEEPDIVE_HOME]/lib/dw_mac/lib
    export DYLD_LIBRARY_PATH=[DEEPDIVE_HOME]/lib/dw_mac
```

On Linux:
  
```bash
    export LD_LIBRARY_PATH=[DEEPDIVE_HOME]/lib/dw_linux/lib:[DEEPDIVE_HOME]/lib/dw_linux/lib64
```
 
###Compiling DeepDive

To compile the system, enter the `DEEPDIVE_HOME` directory and run `make`. This
step involves downloading all the necessary frameworks and libraries used
internally, and may take some time. If the operation completes with success, the
last line of the output should read something similar to: 

    [success] Total time: 110 s, completed Jan 28, 2014 6:31:09 PM

If the setup fails, check the log for detailed error information.

### <a id="ddlib" href="#"></a> Setting up environment variables for DDLib

DDLib is our Python library that provides useful utilities such as "Span" to
manipulate elements in sentences.  To use `ddlib`, `DEEPDIVE_HOME/ddlib` must
be added to the `PATH` or `PYTHONPATH` environment variables:

```bash
	export PYTHONPATH=DEEPDIVE_HOME/ddlib:$PYTHONPATH
```

For more documentation about `ddlib`, please refer to its pydoc.

### Running Tests

The following command, executed from the `DEEPDIVE_HOME` directory, runs the
sanity checks:

```bash
     make test
```
By default, DeepDive uses the currently logged in user and no password to
connect to the database. The user can change these by setting the `PGUSER` and
`PGPASSWORD` environment variables, respectively.

In case of success, the system prints:
  
    [info] All tests passed.
    [success] Total time: 35 s, completed Jan 29, 2014 9:59:57 AM

Congratulations, DeepDive is now running on your system!

Note that for the system to work properly, the `deepdive` folder must *not* be
renamed.

