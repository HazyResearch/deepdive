---
layout: default
title: DeepDive Application's Structure and Operations
---

# DeepDive Application Structure

A DeepDive application is a directory that contains the following files and directories:

## app.ddlog

Schema, extractors, and inference rules written in our higher-level language, [DDlog][], are put in this file.

## deepdive.conf

Extra configuration not expressed in the DDlog program is in this file.
Extractors, and inference rules can be also be written in [HOCON][] syntax in this file, although DDlog is the recommended way.
See the [Configuration Reference](configuration.md) for full details.

## db.url

A URL representing the database configuration is supposed to be stored in this file.
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

Any data to be processed by this application is suggested to be kept under this directory.
<todo>xref</todo>

## udf/

Any user-defined function (UDF) code is suggested to be kept under this directory.
They can be referenced from deepdive.conf with path names relative to the application root.
<todo>xref</todo>

## run/

<todo>xref</todo>
Each run of the DeepDive application has a corresponding subdirectory under this directory whose name contains the timestamp when the run was started, e.g., `run/20150618/223344.567890/`.
All output and log files that belong to the run are kept under that subdirectory.
There are a few symbolic links with mnemonic names to the most recently started run, last successful run, last failed run for handy access.

----

## Other files

### schema.json and schema.sql

Data-Definition Language (DDL) statements for setting up the underlying database tables should be kept in this file.
This may be omitted when the application is written in DDlog.

### input/init.sh

In addition to the data files, there should be an executable script that knows how to load the data here to the database once its tables are created.


[DDlog]: ddlog
[HOCON]: https://github.com/typesafehub/config/blob/master/HOCON.md#readme "Human Optimized Configuration Object Notation"
