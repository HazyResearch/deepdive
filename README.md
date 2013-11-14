## DeepDive Language

### Overview

DeepDive uses [HOCON](https://github.com/typesafehub/config/blob/master/HOCON.md) as its configuration format. HOCON is a superset of JSON, which means that all valid JSON configuration files are valid HOCON configuration files. HOCON supports additional features such as path expression and variable substitution.

### Connection Configuration

DeepDive requires a postgresql-compatible relational data store. 

```
deepdive.global.connection.host : "localhost"
deepdive.global.connection.port : 5432
deepdive.global.connection.db : "deepdive"
deepdive.global.connection.user : "ubuntu"
deepdive.global.connection.password : "password"
```

### Schema Definition

The DeepDive pipelines uses a relational data store. In this section you will define the schema for relations.

```
deepdive.relations.links.schema : { source: String, destination: String, text: String }
deepdive.relations.pages.schema : { url: String, html: Text, content: Text }
# ... 
```

The name of the relation in the example above is links, as defined by the second level atttribute: `schema.[relation_name]`

The supprted data types are `Integer`, `Long`, `String`, `Decimal`, `Float`, `Text`, `Timestamp`, `Boolean`, and `Binary`.


### 2. Data Ingestion

The DeepDive pipeline loads data into a the data store. In this section you will define the data sources your application needs.

TODO: Right now DeepDive assumes all data is already loaded into the data store in the relations defined previously. Later on we will support loading data from files automatically.



### 3. Feature Extractors

Extractors can be an arbitary executable, we will use Python as an example. Let's start with an example. You would define an extractor for entities as follows:

```
deepdive.extractions: {
  entitiesExtractor.output_relation: "entities"
  entitiesExtractor.input: "SELECT * FROM words"
  entitiesExtractor.udf: "sample/sample_entities.py"
  entitiesExtractor.factor.name: "Entities"
  entitiesExtractor.factor.function: "Imply()"
  entitiesExtractor.factor.weight: 1

  # More Extractors...
}
```

- **Output Relation:** Each extraction tuple will be written to the specified output relation. The output relation must be defined in the schema definition and exist in the database.
- **Input:** A SQL statement that defines the input to the extrator. The extractor will receive one tuple at a time, smilar to a MapReduce scheme.
- **UDF** The path of the extractor executable file.
- **Factor Name:** The name of the factor. This is used for debugging purposes.
- **Factor function:** Refer to the section of factor functions below.
- **Factor weight:** Refer to the section of factor weights below.

#### Factor Functions

Currently we support factor functions of the form "A and B -> Z". In other words, if A and B are true, Z, the output variable, is true. An empty imply function means that the assignment to the output variable does not depend on other variables. Examples of Factor functions:

```
# A and B -> Z
entitiesExtractor.factor.function: Imply(entity1_id, entity2_id) 

# -> Z
entitiesExtractor.factor.function: Imply() 
```

When using variables in a factor functions, the variable names must correspond to the column names of the extractor relation.

#### Factor Weights

The weight of a factor can either be assigned manually, or learned by the system. When the weight is learned it may depend on one or more variables. Example of weights are:

```
# Known weight
entitiesExtractor.factor.weight: 1 

# Learn weight, not depending on any variales
entitiesExtractor.factor.weight: ?

# Learn weight, depending on variables.
# The variable names must correspond to column in the extractor relation.
entitiesExtractor.factor.weight: ?(entity1_id)
```


#### Writng Extractors in Python

Extractors work in a MapReduce-like way, mapping each input tuple to one or more output tuples. The extractor executable will receive each input tuple as a JSON object in stdin, and output each output tuple in stdout. An extrator in Python that outputs uppercase words may look like this:

```python
#! /usr/bin/env python

import fileinput
import json

for line in fileinput.input():
  data = json.loads(line)
  # The input is a JSON array with each field defined in the schema definition
  if data[3][0].isupper():
    # The ouput is a JSON array with each field, 
    # except for id, defined in the schema definition
    print json.dumps([int(data[0]), data[3]])
```


### 4. Defining Evidence Relations

TODO 


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

### Actor system structure

- ContextManager
- ExtractionManager: Mabages the execution and ordering of extractors and starts ExtractorExecutors
- ExtractorExecutor: Executes a extractors, one at a time
- FactorGraphBuilder: Notified when the extractors are finished and build the factor graph in the database

### Inference Relation Schema

TODO: What future functionality do we need to support?

```
  variables(id, variable_type, lower_bound, uppper_bound, initial_value)
  factors(id, weight_id, factor_function_id)
  factor_variables(factor_id, variable_id, position, is_positive)
  weights(id, value, is_fixed)
  factor_functions(id, description)
```



