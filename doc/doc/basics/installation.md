---
layout: default
---

# DeepDive Installation Guide 

This document explains how to install DeepDive.

### Dependencies

DeepDive uses PostgreSQL, Scala, and Python (version 2.X). The following
software packages must be installed to run DeepDive:

- [Our DimmWitted Sampler](sampler.html)

<!-- TODO (Zifei) Do we still need to install it separately? -->

- [Java](http://www.oracle.com/technetwork/java/javase/downloads/jre7-downloads-1880261.html)
  (version **1.7.0_45 or higher**)

- [Python](http://www.python.org/getit/) *2.X* (not Python 3.X)
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

### <a name="sampler" href="#"></a> Install the DimmWitted Sampler

<!-- TODO (Ce) What of the following is needed? -->
DimmWitted is our fast sampler that DeepDive use in the sampling step.
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

To compile the system, enter the `DEEPDIVE_HOME` directory and run 

```bash
make
```

This step involves downloading all the necessary frameworks and libraries used
internally, and may take some time. If the operation completes with success, the
last line of the output should read something similar to: 

    [success] Total time: 110 s, completed Jan 28, 2014 6:31:09 PM

<!-- TODO (Zifei) Nobu said that this message is hard to find. Check what we can
do about this. -->

If the setup fails, check the log for detailed error information.

<!-- TODO (Zifei) Where is this error log ? -->

<!-- TODO (Ce) Nobu said he got [warn] messages during the setup. What shall we
tell the users? to ignore them? -->

### Running Tests

The following command, executed from the `DEEPDIVE_HOME` directory, runs the
sanity checks:

```bash
make test
```
DeepDive uses your login username and no password to connect to the database
during the tests. You can change these by setting the `PGUSER` and
`PGPASSWORD` environment variables, respectively.

<!-- TODO (Zifei) Explain how -->

In case of success, the system prints:
  
    [info] All tests passed.
    [success] Total time: 35 s, completed Jan 29, 2014 9:59:57 AM

<!-- TODO (Zifei) Check that this is actually what is printed -->

Congratulations, DeepDive is now running on your system!

Note that for the system to work properly, the `deepdive` folder must *not* be
renamed.

### <a id="ddlib" href="#"></a> Additional step: setting up environment
variables for DDLib

DDLib is our Python library that provides useful utilities such as "Span" to
manipulate elements in sentences. It can be useful to write your UDFs during the
[extraction step](overview.html#extraction). To use `ddlib`,
`DEEPDIVE_HOME/ddlib` must be added to the `PATH` or `PYTHONPATH` environment
variables:

```bash
export PYTHONPATH=DEEPDIVE_HOME/ddlib:$PYTHONPATH
```

For more documentation about `ddlib`, please refer to its pydoc.

<!-- TODO (Zifei) Explain how to access the pydoc -->

