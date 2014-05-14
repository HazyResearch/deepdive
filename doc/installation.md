---
layout: default
---

# Installing DeepDive

This tutorial explains how to install DeepDive on your own machine.

### Dependencies

DeepDive uses PostgreSQL, Scala, and Python (version 2.X), so before downloading the system, please make sure to install the following:

- [Our DimmWitted Sampler](sampler.html)
- [Java](http://www.oracle.com/technetwork/java/javase/downloads/jre7-downloads-1880261.html) (If you already have Java installed, **make sure it is version 1.7.0_45 or higher**)
- [Python](http://www.python.org/getit/) 2.X (not Python 3.X)
- [PostgreSQL](http://wiki.postgresql.org/wiki/Detailed_installation_guides)
- [SBT](http://www.scala-sbt.org/release/docs/Getting-Started/Setup.html)

### Downloading and Compiling DeepDive

Now you are ready to setup DeepDive. You will need Git, so [install Git](http://git-scm.com/book/en/Getting-Started-Installing-Git) if you do not have it. Then, run these commands:
    
    >> cd [desired directory for deepdive]
    >> git clone git@github.com:HazyResearch/deepdive.git

This will create a directory named deepdive. Now go into the directory and compile the system:

    >> cd deepdive
    >> make

This step downloads all the necessary frameworks and libraries the system uses internally, and may take a minute or two. If all goes well the last line of the output should look something like this:

    [success] Total time: 110 s, completed Jan 28, 2014 6:31:09 PM

If you do not see this, check the log for detailed error information.

### Running Tests

Now let's run some sanity check tests. By default, DeepDive uses the currently logged in user and no password to connect to the database. You can change these by setting the `PGUSER` and `PGPASSWORD` environment variables, respectively. Then, run this command from the deepdive directory:

    >> make test

You should see the following output:
  
    [info] All tests passed.
    [success] Total time: 35 s, completed Jan 29, 2014 9:59:57 AM

Congratulations! DeepDive is now running, and you can proceed to the [example application walkthrough](walkthrough.html).
