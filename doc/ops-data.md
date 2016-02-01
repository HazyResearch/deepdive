---
layout: default
title: Managing input data and data products
---

# Managing input data and data products

## Running Queries in DDlog

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



## Running Queries in SQL

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
