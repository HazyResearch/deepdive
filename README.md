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

### 1. Schema Definition

The DeepDive pipelines uses a relational data store. In this section you will define the schema for relations.

```
relations.links.schema : [[Source, String], [Destination, String], [text, String]]
relations.pages.schema : [[Url, String], [html, Text], [content, Text]]
relations.pages.dumo : False
# ... 
```

The name of the relation in the example above is links, as defined by the second level atttribute: `schema.[relation_name]`

The supprted data types are `Integer`, `String`, `Decimal`, `Float`, `Text`, `Timestamp`, `Boolean`, and `Binary`.


### 2. Data Ingestion

The DeepDive pipeline loads data into a the data store. In this section you will define the data sources your application needs.

```
# A "links" relation backed by a TSV file
ingest.links.source : "data/links.tsv"
ingest.links.destination : "links"
```

The input format is inferred by looking at the file extension. DeepDive currently support `.csv` and `.tsv` files.


### 3. Feature Extraction

Next, you will define how to generate feature relations. There are several ways this can be done:

- Writing Datalog rules.
- Running extractors over your relations. Extractors can be written in Python (more languages coming). The input to an extractor is a relation defined by a SQL statement, and the function maps each tuple to a new tuple in the output relation.

Extractors should be saved in the `extractor/` directory. We automatically parse each file in this directory and look for classes matching the definitions defined below. (TODO: Is there a better way?)

#### Using Datalog

TODO: Which Datalog features do we need to support?

```
# Defines a new twoHop relation. The destination is reachable only by going through at least one additional hop.
features.twoHop.type = "datalog"
features.twoHop.expression = "twoHop(Source, Destination) :- link(Source, Z), link(Z, Destination), !link(Source, Desintation)"
```

#### Defining extractors in Python

When using Python functions as feature extractors the relation is loaded into memory and each tuple is mapped to a new output tuple. A Python feature extractor class must adhere to the following interface:

```python
class MyExtractor(FeatureExtractor):
  def map(tuple):
    for tuple in input_relation:
      # do something
      return output_tuple
```

TODO: Do we need to be able to take teh whole relation as an input to enable in-memory grouping, or is the mapreduce-like schema good enough?

You define a Python extractor in the configuration as follows:

```
features.twoHop.type = "python"
features.twpHop.input_sql = "SELECT * FROM links"
features.twoHop.extractor = "MyExtractor"
```

### 3. Statistical Inference

Can we keep what we have right now? 


## Developer Guide & System Architecture 

DeepDive is written in [Scala (2.10)](http://www.scala-lang.org/) and makes heavy use of the [Akka](http://akka.io/) framework. We use [Slick](http://slick.typesafe.com/) for RDBMS access. Unit- and integration tests are written using [Scalatest](http://www.scalatest.org/). DeepDive uses [sbt](http://www.scala-sbt.org/) for dependency management.

Please follow the [Scala Style Guide](http://docs.scala-lang.org/style/) when contributing.

### Compiling 

```shell
sbt ~compile
```

Note: The "~" means that sbt will watch files and automatically recompile when a file changes. Useful during development.

### Running Tests

```shell
sbt ~test
```

### Actor System

- ContextManager
- ExtractorManager: Manages ExtractorExecutors
- ExtractorExecutor: Executes a single extractor 
- InferenceManager: Adds variables and Factors to the inference schema





