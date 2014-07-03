---
layout: default
---

# Quick Start

In this documentation, we will use a built-in example (spouse example) to give a basic idea of what DeepDive do and what result DeepDive will produce.

To know details of how to build your own application like this spouse example, please see the [Walkthrough](walkthrough.html).

Before starting to use DeepDive, make sure you have installed it following this [Installation Tutorial](installation.html) and have started your Postgresql server.

## Data and Goal

In this example, we will work on a news articles dataset which are in normal 'TXT format'. Our goal is to extract spouse relationships between people from the raw text. That is, we expect DeepDive to give us a result which can help us know who is married to who.

## Run the Application

DeepDive can help you extract the marriage relationship from raw text.

Assume your DeepDive directory is '$DEEPDIVE_HOME':

```bash
cd $DEEPDIVE_HOME/examples/spouse_example
```

This is the application directory we will working in. The data is in the 'data/' directory. The news articles are in the 'data/articles_dump.csv' file, which is transformed from the TXT file. Have a look at this file to get a rough idea on what data DeepDive is processing.

Then change to 'default_extractor' directory. This will run the application using the default 'json_extractor'. Other extractors like 'tsv_extractor' and 'plpy_extractor' can also be found in the application directory. For more on extractors, please see [Writing Extractors](extractors.html).

```bash
cd default_extractor
```

Run the application:

```bash
./run.sh
```

It will take about 100 seconds to run. If succeed, you will see the report similar to:

    13:05:28 [profiler] INFO  --------------------------------------------------
    13:05:28 [profiler] INFO  Summary Report
    13:05:28 [profiler] INFO  --------------------------------------------------
    13:05:28 [profiler] INFO  ext_clear_table SUCCESS [137 ms]
    13:05:28 [profiler] INFO  ext_people SUCCESS [7087 ms]
    13:05:28 [profiler] INFO  ext_has_spouse_candidates SUCCESS [6444 ms]
    13:05:28 [profiler] INFO  ext_has_spouse_features SUCCESS [37349 ms]
    13:05:28 [profiler] INFO  inference_grounding SUCCESS [34316 ms]
    13:05:28 [profiler] INFO  inference SUCCESS [14779 ms]
    13:05:28 [profiler] INFO  calibration plot written to $DEEPDIVE_HOME/out/2014-06-23T130346/calibration/has_spouse.is_true.png [0 ms]
    13:05:28 [profiler] INFO  calibration SUCCESS [1133 ms]
    13:05:28 [profiler] INFO  --------------------------------------------------
    13:05:28 [taskManager] INFO  Completed task_id=report with Success(Success(()))
    13:05:28 [taskManager] INFO  1/1 tasks eligible.
    13:05:28 [taskManager] INFO  Tasks not_eligible: Set()
    [success] Total time: 103 s, completed Jun 23, 2014 1:05:28 PM
	
## Result

All the result are in the database 'deepdive_spouse_default'. You can make any query you like to look around the result. For example:

```bash
psql -d deepdive_spouse_default -c "
  SELECT description, expectation
  FROM has_spouse_is_true_inference
  WHERE expectation > 0.9 and expectation < 1
  ORDER BY random() LIMIT 5;
"
```

will give you the marriage relationships that have probability more than 0.9 to be true. The description is formatted as 'person1-person2' like:

```
  description   | expectation 
----------------+-------------
 Obama-Michelle |       0.982 
```

This means that 'Obama' is married to 'Michelle' with probability of 0.982.

To evaluate the quality of the result, we also generate the calibration plot. The location of the plot is given in the summary report of each run

    13:05:28 [profiler] INFO  calibration plot written to $DEEPDIVE_HOME/out/2014-06-23T130346/calibration/has_spouse.is_true.png [0 ms]
	
For more about how to understand calibration plot, see [Interpreting calibration plot](general/calibration.html).