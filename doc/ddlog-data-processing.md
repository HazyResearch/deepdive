---
layout: default
title: Writing Applications in DDlog
---

# Writing Applications in DDlog

DDlog is a higher-level language for writing DeepDive applications in succinct Datalog-like syntax.
We are gradually extending the language to allow expression of advanced SQL queries used by complex extractors as well as a variety of factors with Markov Logic-like syntax.
A reference for ddlog lanugage features can be found [here](https://github.com/HazyResearch/ddlog/wiki/DDlog-Language-Features).

## Writing a DDlog Program

A DDlog program consists of the following parts:

1. Schema declarations (eg. relational tables)
2. Data transformation rules
  a. UDFs
  b. += operator TODO : How do you call this?


All DDlog code should be placed in a file named `app.ddlog` under the DeepDive application.
A complete example written in DDlog can be found from [examples/spouse_example/postgres/ddlog](https://github.com/HazyResearch/deepdive/blob/master/examples/spouse_example/postgres/ddlog)


### Basic Syntax

Each declaration and rule ends with a period (`.`).
The order of the rules have no meaning.
Comments in DDlog begin with a hash (`#`) character.

### Schema Declaration

First of all, we declare the relations we use throughout the program.  These relations refer to tables that will appear in your database.  See [deepdive sql TODO](fill in with real link) for details on how to interact with the database.
The order doesn't matter, but it's a good idea to place them at the beginning because that makes it easier to understand.

#### Relations Syntax
```
relation_name(
  column1_name  column1_type,
  column2_name  column2_type,
  ...).
```
Similar to a SQL table definition, a schema declaration is just the name of a relation followed by a comma separated list of the columns and their types.

Below is a real example from [the spouse example TODO] (add real link here).
```
article(
  id int,
  length int,
  author text,
  words text[]).
```
Here we are defining a relation named "article" with four columns, named "id", "length", "author" and "words" respectively. Each column has it's own type, here utilizing `int`, `text` and `text[]`.
[//]:  Comment?

#### Variable Relations
Finally, we declare a variable relation that we want DeepDive to predict the marginal probability for us.

<script defer src="https://gist-it.appspot.com/github.com/HazyResearch/deepdive/blob/master/examples/spouse_example/postgres/ddlog/app.ddlog?footer=minimal&slice=39:41">
</script>

### Candidate Mapping and Supervision Rules
A user-defined function for mapping candidates and supervision them is written in Python and declared as below with a name `ext_people`.
The function is supposed to take as input a triple of sentence id, array of words, and array of NER tags of the words and output rows like the `people_mentions` relation.
The Python implementation `udf/ext_people.py` takes the input as tab-separated values in each line and  outputs in the same format.

<script defer src="https://gist-it.appspot.com/github.com/HazyResearch/deepdive/blob/master/examples/spouse_example/postgres/ddlog/app.ddlog?footer=minimal&slice=41:45">
</script>

Then this user-defined function `ext_people` can be called in the following way to add more tuples to `people_mentions` taking triples from the `sentences` relation:

<script defer src="https://gist-it.appspot.com/github.com/HazyResearch/deepdive/blob/master/examples/spouse_example/postgres/ddlog/app.ddlog?footer=minimal&slice=45:49">
</script>

In a similar way, we can have another UDF map candidate relationships and supervise them.

<script defer src="https://gist-it.appspot.com/github.com/HazyResearch/deepdive/blob/master/examples/spouse_example/postgres/ddlog/app.ddlog?footer=minimal&slice=49:58">
</script>

### Feature Extraction Rules
Also, we use a UDF to extract features for the candidate relationships.

<script defer src="https://gist-it.appspot.com/github.com/HazyResearch/deepdive/blob/master/examples/spouse_example/postgres/ddlog/app.ddlog?footer=minimal&slice=58:69">
</script>

### Inference Rules
Now we need to generate variables from the candidate relation. This is through
the *scoping rule*/*supervision rule* below. In the scoping rule, variables are generated from the
body and distinct on the key given in the head. The supervision for the variables
is annotated with `@label(l)`.

<script defer src="https://gist-it.appspot.com/github.com/HazyResearch/deepdive/blob/master/examples/spouse_example/postgres/ddlog/app.ddlog?footer=minimal&slice=69:72">
</script>

Finally, we define a binary classifier for our boolean variable `has_spouse`.
<script defer src="https://gist-it.appspot.com/github.com/HazyResearch/deepdive/blob/master/examples/spouse_example/postgres/ddlog/app.ddlog?footer=minimal&slice=72:77">
</script>
In this rule, we define a classifier based on the features, and the weight is tied
to the feature.
