## Configuration

### Overview

DeepDive uses [HOCON](https://github.com/typesafehub/config/blob/master/HOCON.md) as its configuration format. HOCON is a superset of JSON, which means that all valid JSON configuration files are valid HOCON configuration files. HOCON supports additional features such as path expression and variable substitution.

Configuration is read from the `config/` directory by merging all files ending in `.conf`. Therefore, you can organize your configuration files as you wish. You may have one large configuration file, or one hundred smaller configuration files. We recommend having at least separate configuration files for each of the sections described below.

### Global Configuration

DeepDive requires a postgresql-compatible relational data store. 

```
global.connection.host : "localhost"
global.connection.port : 5432
globa.connection.db : "deepdive"
global.connection.user : "ubuntu"
global.connection.password : "password"
```

### 1. Data Ingestion

The DeepDive pipeline loads data into a the data store. In this section you will define the data sources your application needs.

```
# A "links" relation backed by a TSV file
ingest.links.source : "data/links.tsv"
ingest.links.schema : [[Source, String], [Destination, String], [text, String]]
```

The name of the relation in the example above is products, as defined by the second level atttribute: `ingest.[relation_name].source: ... `

The supprted data types are `Integer`, `String`, `Decimal`, `Float`, `Text`, `Timestamp`, `Boolean`, and `Binary`.

The input format is inferred by looking at the file extension. DeepDive currently support `.csv` and `.tsv` files.


### 2. Feature Extraction

Next, you will define how to generate feature relations. There are several ways this can be done:

- By defining new relations with Datalog rules.
- By running user-defined functions (UDFs) over your relations. UDFs can be written in SQL, Python, or C.

#### Using Datalog

TODO 

```
# Defines a new twoHop relation. The destination is reachable only by going through at least one additional hop.
features.twoHop.type = "datalog"
features.twoHop.expression = "twoHop(Source, Destination) <- link(Source, Z), link(Z, Destination), !link(Source, Desintation)"
```

#### Defining UDFs in SQL

TODO

#### Defining UDFs in Python

TODO

#### Defining UDFs in C

TODO







