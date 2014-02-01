---
layout: default
---

# DeepDive Installation Guide

### Dependencies

DeepDive uses PostgreSQL, Scala, and Python (version 2.X), so before downloading the system, please make sure to install the following:

- [Java](http://www.oracle.com/technetwork/java/javase/downloads/jre7-downloads-1880261.html) (If you already have Java installed, make sure it is version 6 or higher)
- [Python](http://www.python.org/getit/) 2.X (not Python 3.X)
- [PostgreSQL](http://wiki.postgresql.org/wiki/Detailed_installation_guides)
- [SBT](http://www.scala-sbt.org/release/docs/Getting-Started/Setup.html)

### Downloading and Compiling DeepDive

Now you are ready to setup DeepDive. You will need git, so [install](http://git-scm.com/book/en/Getting-Started-Installing-Git) it if you do not have it. Then, run these commands:
    
    >> cd [desired directory for deepdive]
    >> git clone https://github.com/dennybritz/deepdive.git

This will create a directory named deepdive. Now go into the directory and compile the system:

    >> cd deepdive
    >> sbt compile

This step downloads all the necessary frameworks and libraries the system uses internally, and may take a minute or two. If all goes well the last line of the output should look something like this:

    [success] Total time: 110 s, completed Jan 28, 2014 6:31:09 PM

If you do not see this, check the log for detailed error information.

### Running Tests

Now let's run some sanity check tests. By default, DeepDive uses the currently logged in user and no password to connect to the database. You can change these by setting the `PG_USER` and `PG_PASSWORD` environment variables, respectively. Then, run this command from the deepdive directory:

    >> ./test.sh

You should see the following output:
  
    [info] All tests passed.
    [success] Total time: 35 s, completed Jan 29, 2014 9:59:57 AM

Congratulations! DeepDive is now running, and you can proceed to the [example application walkthrough](/doc/example.html)