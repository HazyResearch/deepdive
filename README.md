## DeepDive Configuration

### Overview

DeepDive uses [HOCON](https://github.com/typesafehub/config/blob/master/HOCON.md) as its configuration format. HOCON is a superset of JSON, which means that all valid JSON configuration files are valid HOCON configuration files. HOCON supports additional features such as path expression and variable substitution, and easy nesting.

### Connection Configuration

DeepDive requires a postgresql-compatible relational data store. 

```
deepdive.global.connection {
  host : "localhost"
  port : 5432
  db : "deepdive"
  user : "ubuntu"
  password : "password"
}
```

### Schema Definition

Deepdive uses a relational schema which must be defined in the configuration file. 

```
deepdive.relations {
  titles.schema: { id: Long, title: Text, has_entities: Boolean }
  titles.query_field : "has_entities"
  words.schema { id: Long, title_id: Integer word: String }
  words.fkeys { title_id: titles.id }
  # ... 
}
```

The above defines two relations, *titles* and *words*.  The supprted data types are `Long`, `String`, `Decimal`, `Float`, `Text`, `Timestamp`, `Boolean`, and `Binary`. These data types will be mapped to database-specific types by DeepDive.

#### Foreign Keys

Foreign keys are used to declare dependencies between relations. If a relation is populated by an extractor (see below), then all its parent relations must have been populated previously. 

#### Query and Evidence Fields

A relation can contain both evidence and query tuples. Tuples are used as evidence if their *query_field* is not null. In that case, the value of the query_field is used as the value for the variable in statistical learning. If the *query_field*  is null, then DeepDive will user probabilitic inference to predict its value. In the above example, we are trying to predict if a title contains entities. 

Each variable that is used inside a factor function (see below) must be defined to be a *query_field*. We have at most one query field per relation.

### 2. Data Ingestion (not yet supported)

Currently DeepDive assumes that all intiial data is already stored in the database, and that you have defined appropriate relations in the configuration. We are planning to support automatic data ingestion in the future.


### 3. Feature Extraction

A feature extractor takes data defined by an arbitary SQL query as input, and produces new tuples as ouput which are written to the *output_relation*. The function for this transformation is defined by the *udf* setting, which can be an arbitary executable. 

The following examples 


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

#### Writing Extractors

DeepDive will stream tuples into an extract in JSON format, one per line. In the example above, each line may look as follows:

```JSON
{ id: 5, title: "I am a title", has_entities: true }
```

The extractor should output tuples in JSON format to stdout in the same way, but without the `id` field, which is automatically assigned:

```JSON
{ title_id: 5, word: "I" } 
{ title_id: 5, word: "am" } 
{ title_id: 5, word: "a" } 
{ title_id: 5, word: "title" } 
```

Such an extractor could be written in Python as follows:

```python
#! /usr/bin/env python

import fileinput
import json

# For each tuple..
for line in fileinput.input():
  # From: titles(id, title, has_extractions)
  # To: words(id, title_id, word)
  row = json.loads(line)
  # We are emitting one variable and one factor for each word.
  if row["titles.title"] is not None:
    # print json.dumps(list(set(title.split(" "))))
    for word in set(row["titles.title"].split(" ")):
      # (title_id, word) - The id is automatically assigned.
      print json.dumps({"title_id": int(row["titles.id"]), "word": word})
```

#### Ordering of Extractor Execution

DeepDive uses the foreign keys defined in the relations section to impose an ordering on extractor execution. If relation A has a foreign key to relation B, then the extractor that populates B will be executed before the extractor the populates A. When there is no depedency path between extractors, then DeepDive may execute them in parallel to increase performance.

### Rules (Factors)

Domain-specific rules are defined as *factors*. Each factor has a name, a factor function, and a weight associated with it. The factor function encodes the semantics of the rule.

#### Factor Functions

Currently, DeepDive only supports factor functions of the form "A and B and ... -> Z". In other words, if A, B, ... are true then the output variable Z is true. An empty imply function ( -> Z) means that the assignment of the output variable does not depend on other variables.

Examples of Factor functions:

```
someExtractor.factor.function: id = Imply(relation1.someQueryField, relation3.someQueryField) 
someExtractor.factor.function: id = Imply() 
```

All variables used in a factor function must correspond to *query_fields* defined in the relation configuration.


#### Factor Weights

Factor weights describe how much weight we put on a rule (factor function). DeepDive uses evidence to automatically learn the weights for factor functions, but weights can also assigned manually. A weight can be a function of existing variables. In that case the weight will be different based on the assignment of its variables. The following are examples of weight definitions:

```
# Known weight
entitiesExtractor.factor.weight: 1 
# Learn weight, not depending on any variales
entitiesExtractor.factor.weight: ?
# Learn weight, depending on variables.
entitiesExtractor.factor.weight: ?(entity1_id)
```

## Developer Guide & System Architecture 

DeepDive is written in [Scala (2.10)](http://www.scala-lang.org/) and makes heavy use of the [Akka](http://akka.io/) framework. We use [Anorm](http://www.playframework.com/documentation/2.2.1/ScalaAnorm) for RDBMS access. Unit- and integration tests are written using [Scalatest](http://www.scalatest.org/). DeepDive uses [sbt](http://www.scala-sbt.org/) for dependency management.

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

### Factor Graph Database Schema

```
weights(id, initial_value, is_fixed)
factors(id, weight_id, factor_function)
variables(id, data_type, initial_value, is_evidence, is_query)
factor_variables(factor_id, variable_id, position, is_positive)
```

### Factor Graph Output Schema

The systems outputs three **tab-separated** files for weights, factors and variables, with the following schemas, respectively:

```
weight_id initial_value is_fixed
1013  0.0 false
585 0.0 false
```

```
factor_id weight_id factor_function
251 187 ImplyFactorFunction
2026  382 ImplyFactorFunction
```

```
variable_id factor_id position  is_positive data_type initial_value is_evidence is_query
0 0 1 true  Discrete  0.0 true  false
1 1 1 true  Discrete  0.0 true  false
2 2 1 true  Discrete  0.0 true  false
```

Note the the last file is effectively a map between variables and factors. This means that a variable id can appear multiple times. `data_type` `initial_value` `is_evidence` `is_query` are properties of a variable as opposed to properties the variable mapping, thus these are equal for a variable id and may be redundant.



