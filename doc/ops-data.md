---
layout: default
title: Managing input data and data products
---

# Managing input data and data products

DeepDive provides a handful of commands and conventions to manage input data to an application as well as the data produced by it.
The actual processing of the data is discussed in a page about [executing the DeepDive application](ops-execution.md).

## Inspecting schema for relations in the DeepDive app

There are a few handy ways to inspect the relational schema declared for a DeepDive app.
Once the application is compiled, the following commands allow you to access the schema.

### Listing relations

```bash
deepdive relation list
```

```
articles
has_spouse
num_people
person_mention
sentences
spouse_candidate
spouse_feature
spouse_label
spouse_label__0
spouse_label_resolved
spouses_dbpedia
```

### Listing columns of a relation

```bash
deepdive relation columns articles
```

```
id:text
content:text
```

### Listing variable relations

```bash
deepdive relation variables
```

```
has_spouse
```


## Preparing the database for the DeepDive app

The database for the DeepDive application is configured through [the `db.url` file](deepdiveapp.md#db-url) or can be overridden with the `DEEPDIVE_DB_URL` environment variable.

### Initializing the database

To initialize the database in a clean state, also dropping it if necessary, run:

```bash
deepdive db init
```

### Creating tables in the database

To make sure an empty table named *foo* is created in the database as declared in `app.ddlog`, run:

```bash
deepdive create table foo
```

It is also possible to create any table with a particular column-type definitions, after another existing table such as *bar*, or using the result of a SQL query.
A view can be created given a SQL query.
This command ensures any existing table or view with the same name is dropped, and a new empty one is created and exists.

```bash
deepdive create table foo x:INT y:TEXT ...
```
```bash
deepdive create table foo like bar
```
```bash
deepdive create table foo as 'SELECT ...'
```
```bash
deepdive create view bar as 'SELECT ...'
```

To create a table only if it does not exist, the following command can be used instead:

```bash
deepdive create table-if-not-exists foo ...
```


## Organizing input data

All input data for a DeepDive application should be kept under the `input/` directory.
DeepDive relies on a naming convention and assumes data for a relation <code>*foo*</code> declared in `app.ddlog` (and not output by an extractor) exists at path <code>input/*foo*.*extension*</code> where <code>*extension*</code> can be one of `tsv`, `csv`, `sql` `tsv.bz2`, `csv.bz2`, `tsv.gz`, `csv.gz`, `tsv.sh`, `csv.sh`. This indicates in what format it is serialized as well as how it is compressed, or whether it's a shell script that emits such data or a file containing the data itself.
For example, in the [spouse example](example-spouse.md), the `input/articles.tsv.sh` is a shell script that produces lines with tab-separated values for the "articles" relation.



## Moving data in and out of the database

### Loading data to the database

To load such input data for a relation *foo*, run:

```bash
deepdive load foo
```

To load data from a particular source such as `source.tsv` or multiple sources `/tmp/source-1.tsv`, `/data/source-2.tsv.bz2` instead, they can be passed over as extra arguments:

```bash
deepdive load foo  source.tsv
```
```bash
deepdive load foo  /tmp/source-1.tsv /data/source-2.tsv.bz2
```

When a data source provides only particular columns, they can be specified as follows:

```bash
deepdive load 'foo(x,y)'  only-x-y.csv
```

If the destination to load is a [variable relation](writing-model-ddlog.md#variable-relations) and no columns are explicitly specified, then the sources are expected to provide an extra column at the right end that corresponds to each row's supervision label.

### Unloading data products from the database

To unload data produced for a relation to files, such as *bar* into two sinks `dump-1.tsv` and `/data/dump-2.csv.bz2`, assuming you have the table populated in the database by [executing some data processing](ops-execution.md), run:

```bash
deepdive unload bar  bar-1.tsv /data/bar-2.csv.bz2
```

This will unload partitions of the rows of relation *bar* to the given sinks in parallel.





## Running queries against the database

### Running queries in DDlog

It is possible to write simple queries against the database in DDlog and run them using the `deepdive query` command.
A DDlog query begins with an optional list of expressions, followed by a separator `?-`, then a typical body of a conjunctive query in DDlog.
Following are examples of actual queries that can be used against the data produced by DeepDive for the [spouse example](example-spouse.md) after running `deepdive run` command at least once.

#### Browsing values

To browse values in a relation, variables can be placed at the columns of interest, such as `name1` and `name2` for the second and fourth columns of the `spouse_candidate` relation, and the wildcard `_` can be used for the rest to ignore them as follows:

```bash
deepdive query '?- spouse_candidate(_, name1, _, name2).'
```

#### Joins, selection, and projections
Finding values across multiple relations that satisfy certain conditions can be expressed in a succinct way.
For example, finding the names of candidate pairs of spouse mentions in a document that contains a certain keyword, such as "President" can be written as:

```bash
deepdive query '
    name1, name2 ?-
        spouse_candidate(p,name1,_,name2),
        person_mention(p,_,doc,_,_,_),
        articles(doc, content),
        content LIKE "%President%".
    '
```

#### Aggregation
Computing aggregate values such as counts and sums can be done using aggregate functions.
Counting the number of tuples is as easy as just adding a `COUNT(1)` expression before the separator.
For example, to count the number of documents that contain the word "President" is written as:

```bash
deepdive query 'COUNT(1) ?- articles(_, content), content LIKE "%President%".'
```

#### Grouping
If expressions using aggregate functions are mixed with other values, the aggregates are grouped by the other ones.
This makes it easy to break down a single aggregate value into parts, such as number of candidates per document as shown below.

```bash
deepdive query '
    doc, COUNT(1) ?-
        spouse_candidate(p,_,_,_),
        person_mention(p,_,doc,_,_,_),
        articles(doc, content).
    '
```

#### Ordering
Ordering the results by certain ways is also easily expressed by adding `@order_by` annotations before the value to use for ordering.
For example, the following modified query shows the documents with the most number of candidates first:

```bash
deepdive query '
    doc, @order_by("DESC") COUNT(1) ?-
        spouse_candidate(p,_,_,_),
        person_mention(p,_,doc,_,_,_),
        articles(doc, content).
    '
```

`@order_by` takes two arguments: 1) `dir` parameter taking either the "ASC" or `"DESC"` for ascending (default) or descending order and 2) `priority` parameter taking a number for deciding the priority for ordering when there are multiple `@order_by` expressions (smaller the higher priority).
For example, `@order_by("DESC", -1)`, `@order_by("DESC")`, `@order_by(priority=-1)` are all recognized.


#### Limiting (top *k*)
By putting a number after the expressions separated by a pipe character, e.g., `| 10`, the number of tuples can be limited.
For example, the following modified query shows the top 10 documents containing the most candidates:

```bash
deepdive query '
    doc, @order_by("DESC") COUNT(1) | 10 ?-
        spouse_candidate(p,_,_,_),
        person_mention(p,_,doc,_,_,_),
        articles(doc, content).
    '
```


#### Using more than one rule
Sometimes one rule is not enough to express, so a query may define multiple temporary relations first.
For example, to produce a histogram of the number of candidates per document, the counts must be counted, so two rules are necessary as shown below.

```bash
deepdive query '
    num_candidates_by_doc(doc, COUNT(1)) :-
        spouse_candidate(p,_,_,_),
        person_mention(p,_,doc,_,_,_),
        articles(doc, content).

    @order_by num_candidates, COUNT(1) ?- num_candidates_by_doc(doc, num_candidates).
    '
```

#### Saving results
By providing the extra `format=tsv` or `format=csv` argument, the resulting tuples can be easily saved into a file.
For example, the following command saves the names of candidates with their document id as a comma-separated file named `candidates-docs.csv`.

```bash
deepdive query '
    doc,name1,name2 ?-
        spouse_candidate(p1,name1,_,name2),
        person_mention(p1,_,doc,_,_,_).
    ' format=csv  >candidates-docs.csv
```

#### Viewing the SQL
Using the `-n` flag will display the SQL query to be executed instead of showing the results from executing it.

```bash
deepdive query -n '?- ...'
```



### Running queries in SQL

```bash
deepdive sql
```

This command opens a SQL prompt for the underlying database configured for the application.

Optionally, the SQL query can be passed as a command line argument to run and print its result to standard output.
For example, the following command prints the number of sentences per document:

```bash
deepdive sql "SELECT doc_id, COUNT(*) FROM sentences GROUP BY doc_id"
```

To get the result as tab-separated values (TSV), or comma-separated values (CSV), use the following commands:

```bash
deepdive sql eval "SELECT doc_id, COUNT(*) FROM sentences GROUP BY doc_id" format=tsv
```
```bash
deepdive sql eval "SELECT doc_id, COUNT(*) FROM sentences GROUP BY doc_id" format=csv header=1
```


