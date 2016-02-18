---
layout: default
title: DeepDive Quick Start
---

# DeepDive Quick Start

DeepDive helps you extract structured knowledge from less-structured data with statistical inference without having to write any sophisticated machine learning code.
Here we show how you can quickly install and run your first DeepDive application.


## Installing DeepDive

First, you can quickly [install DeepDive](installation.md) by running the following command and selecting the `deepdive` option:

```bash
bash <(curl -fsSL git.io/getdeepdive)
```

```
### DeepDive installer for Mac
+ curl -fsSL https://github.com/HazyResearch/deepdive/raw/v0.8.x/util/install/install.Mac.sh
1) deepdive                 4) deepdive_from_source
2) deepdive_examples_tests  5) postgres
3) deepdive_from_release    6) run_deepdive_tests
# Select what to install (enter for all options, q to quit, or a number)? 1
```


You need to have a database instance to run any DeepDive application.
You can select `postgres` from DeepDive's installer to install it and spin up an instance on you machine, or just run the following command:

```bash
bash <(curl -fsSL git.io/getdeepdive) postgres
```

Alternatively, if you have access to a database server, you can configure how to access it as a URL in [the application's `db.url` file](deepdiveapp.md#db-url).



## Running your first DeepDive app

Now, to quickly see what DeepDive can do for us, let's grab a copy of [the spouse example app explained in the tutorial](example-spouse.md).
This app extracts mentions of spouses from [a corpus of news articles][corpus].

[corpus]: http://research.signalmedia.co/newsir16/signal-dataset.html "The Signal Media One-Million News Articles Dataset"

```bash
bash <(curl -fsSL git.io/getdeepdive) spouse_example
```

This will download a copy of [the example app's code and data from GitHub](../examples/spouse/) to a folder named `spouse/`.
So, let's move into it:

```bash
cd spouse
```

Then, check if we have everything there:

```bash
ls -F
```
```
README.md  db.url         input/     mindbender/  udf/	app.ddlog  deepdive.conf  labeling/  
```

### 1. Load input

You can find a few more datasets under `input/` and welcome to [download the full corpus][corpus], but we can quickly proceed with the smallest one that has 100 sampled articles:

```bash
ln -s articles-100.tsv.bz2 input/articles.tsv.bz2
```
```bash
deepdive do articles
```

This will load the input data into the database.
*Note that everytime you use the `deepdive do` command, it will open a list of commands to be run in your text editor to have you confirm it by saving and quiting the editor.*

Here're the first few lines of an example article in the input that has been loaded.

```bash
deepdive query '?- articles("DOC_ID", content).' format=csv | head
```
```
...
```
<todo>find the right DOC_ID to show here</todo>


### 2. Process input

This app uses [Stanford CoreNLP](http://stanfordnlp.github.io/CoreNLP/) to add some useful markups to the English text.
Using the marked up *named entity recognition* (NER) tags, it can tell which parts of the text mention people's names.
All pairs of names appearing in the same sentence are [considered as *candidates* for correct mentions of married couples' names](example-spouse.md#1-3-extracting-candidate-relation-mentions).

```bash
deepdive do sentences
```
```bash
deepdive query '?- sentences("DOC_ID", _, _, tokens, _, _, ner_tags, _, _, _).'
```
```
...
```
<todo>show NER tags for the running example sentence</todo>

```bash
deepdive do spouse_candidate
```
```bash
deepdive query 'name1, name2 ?- spouse_candidate(p1, name1, p2, name2), person_mention(p1, _, "DOC_ID", _, _, _).'
```
```
...
```

For supervised machine learning, the app [extracts *features*](example-spouse.md#1-4-extracting-features-for-each-candidate) from the context of those candidates and [creates a training set](example-spouse.md#3-learning-amp-inference-model-specification) programmatically by finding promising positive and negative examples using [*distant supervision*](distant_supervision.md).

### 3. Run the model

Using the processed data, the app constructs a [statistical inference model](inference.md) to predict whether a mention is a correct mention of spouses or not, estimates the parameters (i.e., learns the weights), and computes their *marginal probabilities*.

```bash
deepdive do probabilities
```

As a result, DeepDive gives the expectation (probability) of every variable being true.
Here's the probability computed for the example pair of names we saw earlier:

```bash
deepdive sql "SELECT p1_name, p2_name, expectation FROM has_spouse_label_inference WHERE p1_id LIKE 'DOC_ID%'"
```
<!-- TODO switch to DDlog once it gets access to inference results -->
```
...
```

DeepDive provides a suite of tools to work with the data produced by the application.
For instance, below is a screenshot of an automatic interactive search interface DeepDive provides for [browsing the processed data with predicted results](browsing.md).

![Screenshot of the search interface provided by Mindbender](images/browsing_results.png)



## Next steps

For more detail about the spouse example we just ran here, continue reading [the tutorial](example-spouse.md).
Other parts of the documentation will help you pick up more [background knowledge](index.md#background-reading) and learn more about [how DeepDive applications are developed](index.md#deepdive-application-development).
They will prepare you to write your own DeepDive application that can shed light on some dark data and unlock knowledge from it!
