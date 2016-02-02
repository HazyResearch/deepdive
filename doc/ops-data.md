---
layout: default
title: Managing input data and data products
---

# Managing input data and data products

<br><todo>Document

- `deepdive load`, `deepdive unload`, `deepdive sql`, `deepdive db` commands
- where input/output data resides
- `input/RELATION.*` convention
- how they are moved-in/out and managed
- (Later, `deepdive snapshot` for PG schema support will be added here.)

</todo>

## Organizing input data

All input data for a DeepDive application should be kept under the `input/` directory.
DeepDive will rely on a naming convention and assume data for a relation <code>*foo*</code> declared in `app.ddlog` exists at path <code>input/*foo*.*extension*</code> where <code>*extension*</code> can be one of `tsv`, `csv`, `tsv.bz2`, `csv.bz2`, `tsv.gz`, `csv.gz`, `tsv.sh`, `csv.sh` to indicate the format as well as what format it is compressed in, or whether it's a shell script that emits such data or a file containing the data itself.
For example, in the [spouse example](example-spouse.md), the `input/articles.tsv.sh` is a shell script that produces lines with tab-separated values for the "articles" relation.


## Loading Data to the Database

To load such input data for a relation *foo*, run:

```bash
deepdive load foo
```

To load data from a particular source such as `/tmp/source.tsv` or multiple sources `/tmp/source-1.tsv`, `/tmp/source-2.tsv.bz2` instead, they can passed over as extra arguments:

```bash
deepdive load foo  /tmp/source-1.tsv /tmp/source-2.tsv.bz2
```


## Unloading Data Products from the Database

```bash
deepdive unload foo  dump-1.tsv dump-2.csv.bz2
```


## Running Queries against the Database

### Running Queries in DDlog

```bash
# easy browsing
deepdive query '?- has_spouse_candidates(_,_,_,desc,id,_).'

# quick counting
deepdive query 'COUNT(id) ?- has_spouse_candidates(_,_,_,desc,id,_).'

# easy joins
deepdive query '
doc ?-
    has_spouse_candidates(_,_,sentence,_,candidate,_),
    sentences(doc, _,_,_,_,_,_,_, sentence).
'

# easy group by
deepdive query '
doc, COUNT(candidate) ?-
    has_spouse_candidates(_,_,sentence,_,candidate,_),
    sentences(doc, _,_,_,_,_,_,_, sentence).
'

# histograms using more than one rule (to group/count twice) and even ordering the result!
deepdive query '
num_candidates_by_doc(doc, COUNT(candidate)) :-
    has_spouse_candidates(_,_,sentence,_,candidate,_),
    sentences(doc, _,_,_,_,_,_,_, sentence).

@order_by num_candidates, COUNT(doc) ?- num_candidates_by_doc(doc, num_candidates).
'

# limiting output (top 10)
deepdive query '
doc, @order_by("DESC") COUNT(candidate) | 10 ?-
    has_spouse_candidates(_,_,sentence,_,candidate,_),
    sentences(doc, _,_,_,_,_,_,_, sentence).
'

# dumping to tsv/csv/etc. format files
deepdive query '?- has_spouse_candidates(_,_,_,desc,id,_).'  format=csv >candidates_desc_id.csv
```

`deepdive query -n DDLOG` shows the SQL to be executed.



### Running Queries in SQL

```bash
deepdive sql
```

This command opens a SQL prompt for the underlying database configured for the application.

Optionally, the SQL query can be passed as a command line argument to run and print its result to standard output.
For example, the following command prints the number of sentences per document:

```bash
deepdive sql "SELECT doc_id, COUNT(*) FROM sentences GROUP BY doc_id"
```

To get the result as tab-separated values (TSV), or comma-separated values (CSV), use the following command:

```bash
deepdive sql eval "SELECT doc_id, COUNT(*) FROM sentences GROUP BY doc_id" format=tsv
deepdive sql eval "SELECT doc_id, COUNT(*) FROM sentences GROUP BY doc_id" format=csv header=1
```


## Other Database Operations

### Initializing the database

```bash
deepdive db init
```

### Creating tables in the database

```bash
deepdive create table foo
deepdive create table foo as 'SELECT ...'
```

```bash
deepdive create view bar as 'SELECT ...'
```

