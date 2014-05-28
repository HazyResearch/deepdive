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
- [Gnuplot](http://www.gnuplot.info/)

### Downloading and Compiling DeepDive

Now you are ready to setup DeepDive. You will need Git, so [install Git](http://git-scm.com/book/en/Getting-Started-Installing-Git) if you do not have it. Then, run these commands:
    
    >> cd [desired directory for deepdive]
    >> git clone https://github.com/HazyResearch/deepdive.git

This will create a directory named deepdive. Now go into the directory and compile the system:

    >> cd deepdive
    >> make

This step downloads all the necessary frameworks and libraries the system uses internally, and may take a minute or two. If all goes well the last line of the output should look something like this:

    [success] Total time: 110 s, completed Jan 28, 2014 6:31:09 PM

If you do not see this, check the log for detailed error information.

<a id="ddlib" href="#"></a>

### Setting up environment variable for DDLib

DDLib is our python library that provides useful utilities such as "Span" to manipulate elements in sentences.

To use `ddlib`, you should have `$DEEPDIVE_HOME/ddlib` added to your `PATH` or `PYTHONPATH`. Add following lines into `env.sh`, if it's not already there:

```bash
# File: env.sh
...
export PYTHONPATH=$DEEPDIVE_HOME/ddlib:$PYTHONPATH
...
```

For more documentation of `ddlib`, please refer to its pydoc.

### Running Tests

Now let's run some sanity check tests. By default, DeepDive uses the currently logged in user and no password to connect to the database. You can change these by setting the `PGUSER` and `PGPASSWORD` environment variables, respectively. Then, run this command from the deepdive directory:

    >> make test

You should see the following output:
  
    [info] All tests passed.
    [success] Total time: 35 s, completed Jan 29, 2014 9:59:57 AM

Congratulations! DeepDive is now running, and you can proceed to the [example application walkthrough](walkthrough.html).

Note that for system to work properly, you cannot rename `deepdive` folder with other names currently.

### Creating a new DeepDive application

Start by creating a new folder `app/testapp` in the `deepdive` directory for your application.

{% highlight bash %}
mkdir -p app/testapp   # make folders recursively
cd app/testapp
{% endhighlight %}

DeepDive's main entry point is a file called `application.conf` which contains database connection information as well as your feature extraction and inference rule pipelines. It is often useful to have a small `run.sh` script that loads environment variables and executes the DeepDive pipeline. We provide simple templates for both of these to copy and modify. Copy these templates to our directory by the following commands: 

<!-- TODO what is env.sh doing? -->

{% highlight bash %}
cp ../../examples/template/application.conf .
cp ../../examples/template/run.sh .
cp ../../examples/template/env.sh .
{% endhighlight %}

The `env.sh` file configures environment variables that will be used in this application. There is a placeholder line `DBNAME=` in that file, modify `env.sh` and fill it with your database name:
  
{% highlight bash %}
# File: env.sh
...
export DBNAME=deepdive_testapp   # modify this line
...
{% endhighlight %}


You can now try executing the `run.sh` file:

{% highlight bash %}
./run.sh
{% endhighlight %}

Because you have not defined any extractors or inference rules you will not see meaningful results, but DeepDive should run successfully from end to end and you should be able to see a summary report such as:

    15:57:55 [profiler] INFO  --------------------------------------------------
    15:57:55 [profiler] INFO  Summary Report
    15:57:55 [profiler] INFO  --------------------------------------------------
