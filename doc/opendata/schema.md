---
layout: default
title: DeepDive Open Datasets
no_toc: true
---

# Schema of DeepDive Open Datasets

All DeeDive Open datasets can be loaded into a relational database
as a table.
This page contains a description of the schema of this table.
Note that the schema of this table
is very similar to what was used in our
[tutorial example](../examples/spouse.md),
but with a few twists to represent more sophisticated provenance
of each word.

Each row of this table is a sentence.
The first column is `doc_id`, which is a text string
specifying the ID of a document; the second column
is `sentence_id`, which is an integer specifying
the ID of a sentence. These two columns together form the primary
key.

The next seven columns are arrays of texts or integers corresponding
to the `ID`, `FORM`, `POSTAG`, `NERTAG`, `LEMMA`, `DEPREL`, and `HEAD`
columns in the standard [CoNLL-X
shared task](https://code.google.com/p/clearparser/wiki/DataFormat#CoNLL-X_format_(conll)).
Each element in an array corresponds to one word in CoNLL's format.

The last column is called `provenance` which is an array of texts
specifying the original position of the corresponding word. Depending
on the type of the original document, provenance has different forms:

  - PDF provenance: If the original document is PDF or (scanned) image, the
  provenance for a word is a set of bounding boxes. For example,
    `[p8l1329t1388r1453b1405, p8l1313t1861r1440b1892],`
  means that the corresponding word appears in page 8 of the original
  document. It has two bounding boxes because it cross two lines.
  The first bounding box has left margin 1329, top margin 1388, right margin
  1453, and bottom margin 1405. All numbers are in pixels when the image
  is converted with 300dpi.

  - Pure-text provenance: If the original document is HTML or pure text,
  the provenance for a word is a set of intervals of character offsets.
  For example, `14/15` means that the corresponding word contains
  a single character that starts at character offset 14 (include)
  and ends at character offset 15 (not include).

