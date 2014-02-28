This example shows how to set up [HSQLDB](http://hsqldb.org/) with DeepDive. Note that this application does not do anything meaningful, it's only purpose is to show the configuration for HSQLDB.

There are several things to note:

### Starting HSQL

To load initial data, you need to start the HSQL server using the `run_hsqldb.sh` script. This will create an in-memory database called `deepdive_hsqldb`, and the server is running on port 9001 by default

### Setting up the databse

The `setup_database.sh` script loads initial data into HSQL. You should execute this script before running the application.

### Inspecting the result

You can inspect the result using the sqltool.jar in the `/lib` directory. Refer to the the [HSQLDB SQLTool guide](http://hsqldb.org/doc/2.0/util-guide/sqltool-chapt.html) for more details.