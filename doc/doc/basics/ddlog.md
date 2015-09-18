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

1. Schema declarations for your source data, intermediate data, and variables.
2. Data transformation rules for extracting candidates, supervision labels, and features.
3. Scope rules for defining the domain of the variables.
4. Inference rules for describing dependency between the variables.

We show how each part should look like using the [spouse example in our walk through](walkthrough/walkthrough.html).
All DDlog code should be placed in a file named `app.ddlog` under the DeepDive application.
A complete example written in DDlog can be found from [examples/spouse_example/postgres/ddlog](https://github.com/HazyResearch/deepdive/blob/master/examples/spouse_example/postgres/ddlog)


### Basic Syntax

Each declaration and rule ends with a period (`.`).
The order of the rules have no meaning.
Comments in DDlog begin with a hash (`#`) character.

### Schema Declaration

First of all, we declare the relations we use throughout the program.
The order doesn't matter, but it's a good idea to place them at the beginning because that makes it easier to understand.

#### Source Relations
We need a relation for holding raw text of all source documents.
Each relation has multiple columns whose name and type are declared one at a time separated by commas.

<script src="https://gist-it.appspot.com/github.com/HazyResearch/deepdive/blob/master/examples/spouse_example/postgres/ddlog/app.ddlog?footer=minimal&slice=1:6">
</script>

#### Intermediate Relations
We'll populate the following `sentences` relation by parsing sentences from every articles and collecting NLP tags.

<script src="https://gist-it.appspot.com/github.com/HazyResearch/deepdive/blob/master/examples/spouse_example/postgres/ddlog/app.ddlog?footer=minimal&slice=6:17">
</script>

Then, we'll map candidate mentions of people in each sentence using the following `people_mentions` relation (ignore `@` annotations).

<script src="https://gist-it.appspot.com/github.com/HazyResearch/deepdive/blob/master/examples/spouse_example/postgres/ddlog/app.ddlog?footer=minimal&slice=18:27">
</script>

Candidate mentions of spouse relationships from them will then be stored in `has_spouse_candidates`.

<script src="https://gist-it.appspot.com/github.com/HazyResearch/deepdive/blob/master/examples/spouse_example/postgres/ddlog/app.ddlog?footer=minimal&slice=27:35">
</script>

For each relationship candidate, we'll extract features.

<script src="https://gist-it.appspot.com/github.com/HazyResearch/deepdive/blob/master/examples/spouse_example/postgres/ddlog/app.ddlog?footer=minimal&slice=35:39">
</script>

#### Variable Relations
Finally, we declare a variable relation that we want DeepDive to predict the marginal probability for us.

<script src="https://gist-it.appspot.com/github.com/HazyResearch/deepdive/blob/master/examples/spouse_example/postgres/ddlog/app.ddlog?footer=minimal&slice=39:41">
</script>

### Candidate Mapping and Supervision Rules
A user-defined function for mapping candidates and supervision them is written in Python and declared as below with a name `ext_people`.
The function is supposed to take as input a triple of sentence id, array of words, and array of NER tags of the words and output rows like the `people_mentions` relation.
The Python implementation `udf/ext_people.py` takes the input as tab-separated values in each line and  outputs in the same format.

<script src="https://gist-it.appspot.com/github.com/HazyResearch/deepdive/blob/master/examples/spouse_example/postgres/ddlog/app.ddlog?footer=minimal&slice=41:45">
</script>

Then this user-defined function `ext_people` can be called in the following way to add more tuples to `people_mentions` taking triples from the `sentences` relation:

<script src="https://gist-it.appspot.com/github.com/HazyResearch/deepdive/blob/master/examples/spouse_example/postgres/ddlog/app.ddlog?footer=minimal&slice=45:49">
</script>

In a similar way, we can have another UDF map candidate relationships and supervise them.

<script src="https://gist-it.appspot.com/github.com/HazyResearch/deepdive/blob/master/examples/spouse_example/postgres/ddlog/app.ddlog?footer=minimal&slice=49:58">
</script>

### Feature Extraction Rules
Also, we use a UDF to extract features for the candidate relationships.

<script src="https://gist-it.appspot.com/github.com/HazyResearch/deepdive/blob/master/examples/spouse_example/postgres/ddlog/app.ddlog?footer=minimal&slice=58:69">
</script>

### Inference Rules
Now we need to generate variables from the candidate relation. This is through
the *scoping rule*/*supervision rule* below. In the scoping rule, variables are generated from the
body and distinct on the key given in the head. The supervision for the variables
is annotated with `@label(l)`.

<script src="https://gist-it.appspot.com/github.com/HazyResearch/deepdive/blob/master/examples/spouse_example/postgres/ddlog/app.ddlog?footer=minimal&slice=69:72">
</script>

Finally, we define a binary classifier for our boolean variable `has_spouse`.
<script src="https://gist-it.appspot.com/github.com/HazyResearch/deepdive/blob/master/examples/spouse_example/postgres/ddlog/app.ddlog?footer=minimal&slice=72:77">
</script>
In this rule, we define a classifier based on the features, and the weight is tied
to the feature.
