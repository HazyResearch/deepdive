---
layout: default
title: DeepDive Application's Structure and Operations
---

# DeepDive application structure

A DeepDive application is a directory that contains the following files and directories:

## app.ddlog

This file can be thought as the blueprint of a DeepDive application.
DDlog declarations and rules written in this file tell DeepDive:

* [What each relation looks like and how one is derived from others.](writing-dataflow-ddlog.md)
* [What user-defined functions are there, what input/output schema they expect, and their implementation details.](writing-udf-python.md)
* [What random variables are to be modeled, how they are correlated.](writing-model-ddlog.md)


## deepdive.conf

Extra configuration not expressed in the DDlog program is in this file.
Extractors, and inference rules can also be written in [HOCON][] syntax in this file, although DDlog is the recommended way.
See the [Configuration Reference](configuration.md) for full details.

## db.url

A URL representing the database configuration is supposed to be stored in this file.
Environment variable `$DEEPDIVE_DB_URL` overrides the URL stored this file.
For example, the following URL can be the line stored in it:

```
postgresql://user:password@localhost:5432/database_name
```

### PostgreSQL SSL

[SSL connections for PostgreSQL](https://jdbc.postgresql.org/documentation/91/ssl.html) can be enabled by setting parameter `ssl` as true in the URL, e.g.:

```
postgresql://user:password@localhost:5432/database_name?ssl=true
```

If you use a self-signed certificate, you may want to disable validation with an extra `sslfactory` parameter:

```
postgresql://user:password@localhost:5432/database_name?ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory
```

## input/

Any [data to be processed by this application](ops-data.md#organizing-input-data) is suggested to be kept under this directory.

## udf/

Any [user-defined function (UDF) code](writing-udf-python.md) is suggested to be kept under this directory.
They can be referenced from deepdive.conf with path names relative to the application root.

## run/

Each [run/execution of the DeepDive application](ops-execution.md) has a corresponding subdirectory under this directory whose name contains the timestamp when the run was started, e.g., `run/20150618/223344.567890/`.
All output and log files that belong to the run are kept under that subdirectory.
There are a few symbolic links with mnemonic names to the most recently started run (`run/LATEST`), last successful run (`run/FINISHED`), last failed run (`run/ABORTED`) for handy access.

----

## Other files

### schema.json and schema.sql

Data-Definition Language (DDL) statements for setting up the underlying database tables should be kept in this file.
This may be omitted when the application is written in DDlog.

### input/init.sh

In addition to the data files, there should be an executable script that knows how to load the data here to the database once its tables are created.


[DDlog]: ddlog
[HOCON]: https://github.com/typesafehub/config/blob/master/HOCON.md#readme "Human Optimized Configuration Object Notation"
