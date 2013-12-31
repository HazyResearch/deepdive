# Writing Extractors

#### Defining extractors in the configuration file

A feature extractor takes data defined by an arbitary SQL query as input, and produces new tuples as ouput. These tuples are written to the *output_relation*. The function for this transformation is defined by the *udf* setting, which can be an arbitary executable (more on that below)

A simple extractor definition looks as folllows:

    deepdive.extractions: {
      wordsExtractor.output_relation: "words"
      wordsExtractor.input: "SELECT * FROM titles"
      wordsExtractor.udf: "words.py"
      # More Extractors...
    }


#### Extractor Dependencies

You can specify dependencies for your extractors as follows:

    deepdive.extractions: {
      wordsExtractor.dependencies: ["anotherExtractorName"]
    }

Extractors will be executed in order of their dependencies. If the dependencies of several extractors ar satisfied at the same time, these may be executed in parallel, or in any order.


#### Extractor parallelism and input batch size

To improve performance, you can specify the number of processes and the inptu batch size for each extractor. Your executable script will be run on N threads in parallel and data will be streamed to this processes in a round-robin fashion. By default each extractor uses 1 process and a batch size of 1000.
    
    # Start 5 processes for this extractor
    wordsExtractor.parallelism: 5
    # Stream 1000 tuples to each process in a round-robin fashion
    wordsExtractor.input_batch_size: 1000


#### Extractor output batch size

To improve performance when writing extracted data back to the database you can optionally specify an `output_batch_size` for each extractor. The output batch size specifies how many extracted tuples we insert into the database at once. For example, if your tuples are very large, a smaller batch size may help avoid out-of-memory errors. The default value is 10,000.

    # Insert each 5000 tuples into the data store
    wordsExtractor.output_batch_size: 5000


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
