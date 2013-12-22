# Deepdive Configuration

DeepDive uses [HOCON](https://github.com/typesafehub/config/blob/master/HOCON.md) as its configuration format. HOCON is a superset of JSON, which means that all valid JSON configuration files are valid HOCON configuration files. HOCON supports additional features such as path expression and variable substitution, and easy nesting.

### Connection Configuration

DeepDive uses a postgresql-compatible relational data store. 

    deepdive.connection {
      url: "jdbc:postgresql://localhost/deepdive_titles"
      user: "deepdive"
      password : "password"
    }

<!-- ### Schema Definition

Deepdive uses a relational schema which must be defined in the configuration file. 

    deepdive.relations {
      titles.schema: { id: Long, title: Text, has_entities: Boolean }
      words.schema { id: Long, title_id: Integer word: String, is_present: True }
    }

The above defines two relations, *titles* and *words*.  The supprted data types are `Long`, `String`, `Double`, `Text`, `Boolean`, and `Binary`. These data types will be mapped to database-specific types by DeepDive.

### 2. Data Ingestion (not yet supported)

Currently DeepDive assumes that all intiial data is already stored in the database, and that you have defined appropriate relations in the configuration. We are planning to support automatic data ingestion in the future. -->

### 3. Feature Extraction

A feature extractor takes data defined by an arbitary SQL query as input, and produces new tuples as ouput which are written to the *output_relation*. The function for this transformation is defined by the *udf* setting, which can be an arbitary executable. 

Extractors can be an arbitary executable, but we will use Python as an example. Let's start with an example. You would define an extractor for words as follows:

    deepdive.extractions: {
      wordsExtractor.output_relation: "words"
      wordsExtractor.input: "SELECT * FROM titles"
      wordsExtractor.udf: "words.py"
      # More Extractors...
    }

#### Writing Extractors

DeepDive will stream tuples into an extract in JSON format, one per line. In the example above, each line may look as follows:

    { id: 5, title: "I am a title", has_entities: true }

The extractor should output tuples in JSON format to stdout in the same way, but without the `id` field, which is automatically assigned.


    { title_id: 5, word: "I", is_present: true } 
    { title_id: 5, word: "am", is_present: true } 
    { title_id: 5, word: "a", is_present: true } 
    { title_id: 5, word: "title", is_present: true } 

**Note: You must output all fields in each json row, even if they are null. **


Such an extractor could be written in Python as follows:

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
          print json.dumps({"title_id": int(row["titles.id"]), "word": word, "is_present": True})


#### Ordering of Extractor Execution

You can specify dependencies for your extractors as follows:

    deepdive.extractions: {
      wordsExtractor.dependencies: ["anotherExtractorName"]
    }

Extractors will be executed in order of their dependencies. If the dependencies of several extractors ar satisfied at the same time, these may be executed in parallel, or in any order.

#### Parallel extractor execution

To improve performance, you can specify the number of processes and the batch size for each extractor. Your executable script will be run on N threads in parallel and data will be streamed to this processes in a round-robin fashion. By default each extractor uses 1 process and a batch size of 1000.
    
    # Start 5 processes for this extractor
    wordsExtractor.parallelism: 5
    # Stream 1000 tuples to each process in a round-robin fashion
    wordsExtractor.batch_size: 1000


## Defining Rules (Factors)

Domain-specific rules are defined as *factors*. Each factor has a name, an input query, a factor function, and a weight associated with it. The factor function encodes the semantics of the rule.

### Factor Input Query

The input query of a factor combines all exracted features the factor is using. It usually is a join query using the feature relations produced by the extractors.

### Factor Functions

Currently, DeepDive only supports factor functions of the form "A and B and ... -> Z". In other words, if A, B, ... are true then the output variable Z is true. An empty imply function ( -> Z) means that the assignment of the output variable does not depend on other variables.

Examples of Factor functions:

    someExtractor.factor.function: titles.has_extractions = Imply(words.is_present) 
    someExtractor.factor.function: words.is_present = Imply()

All variables used in a factor function must be fields made available by the input query. 

### Factor Weights

Factor weights describe how much weight we put on a rule (factor function). DeepDive uses evidence to automatically learn the weights for factor functions, but weights can also assigned manually. A weight can be a function of existing variables. In that case the weight will be different based on the assignment of its variables. The following are examples of weight definitions:

    # Known weight
    wordsExtractor.factor.weight: 1 
    # Learn weight, not depending on any variales
    wordsExtractor.factor.weight: ?
    # Learn weight, depending on variables.
    wordsExtractor.factor.weight: ?(words.text)


### Example

A complete factor definition may look as follows:

    deepdive.factors: {
      words.input_query:  "SELECT words.*, titles.* FROM words INNER JOIN titles ON words.title_id = titles.id"
      words.function: "titles.has_extractions = Imply(words.is_present)"
      words.weight: "?(words.word)"
    }


## Calibration

You can specify an optional holdout fraction in the settings as follows:
  
    deepdive.calibration: {
      holdout_fraction: 0.25
    }

The system generated two types of data files for calibration purposes.

1. The number of variabless for 10 probability buckets from 0.0 to 1.0. This file is generated regardless of whether you specify a holdout in the configuration or not. It can be found in `calibration_data/counts_[relation_name]_[column_name].tsv`. It contains three columns: *bucket_lower_bound*, *bucket_upper_bound*, *number_of_variables*.

2. The number of correct and incorrect predictions for 10 probability buckets from 0.0 to 1.0. This file can be found in `calibration_data/precision_[relation_name]_[column_name].tsv`. It contains four columns: *bucket_lower_bound*,  *bucket_upper_bound*, *number_of_variables_correct*, *number_of_variables_incorrect*. This file only contains useful data if you specify a holdout section in the configuration.

## Sampler Settings

You can optionally parse command line arguments (such as -Xmx) to the sampler binary.

    deepdive.sampler.java_args: "-Xms1g -Xmx8g"
    deepdive.sampler.sampler_args: "-l 1000 -s 10 -i 1000 -t 4"


## Full Example

A full example for a logistic regression title classifier can be found in `examples/titles`.