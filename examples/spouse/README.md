# DeepDive Tutorial <small>Extracting mentions of spouses from the news</small>

In this tutorial, we show an example of a prototypical task that DeepDive is often applied to:
extraction of _structured information_ from _unstructured or 'dark' data_ such as web pages, text documents, images, etc.
While DeepDive can be used as a more general platform for statistical learning and data processing,
most of the tooling described herein has been built for this type of use case,
based on our experience successfully applying DeepDive to [a variety of real-world problems of this type](http://deepdive.stanford.edu/doc/showcase/apps.html).

In this setting, our goal is to take in a set of unstructured (and/or structured) inputs,
and populate a relation database table with extracted outputs,
along with marginal probabilities for each extraction representing DeepDive's confidence in the extraction.
More formally, we write a DeepDive application to extract mentions of _relations_ and their constituent _entities_ or _attributes_, according to a specified schema;
this task is often referred to as **_relation extraction_**.*
Accordingly, we'll walk through an example scenario where we wish to extract mentions of two people being spouses from news articles.

The high-level steps we'll follow are:

1. **Data processing.** First, we'll load the raw corpus, add NLP markups, extract a set of _candidate_ relation mentions, and a sparse _feature_ representation of each.

2. **Distant supervision with data & rules.** Next, we'll use various strategies to provide _supervision_ for our dataset, so that we can use machine learning to learn the weights of a model.

3. **Learning & inference: model specification.** Then, we'll specify the high-level configuration of our _model_.

4. **Error analysis & debugging.** Finally, we'll show how to use DeepDive's labeling, error analysis and debugging tools.

*_Note the distinction between extraction of facts and mentions of facts. In this tutorial, we do the latter, however DeepDive supports further downstream methods for tackling the former task in a principled manner._






## 0. Preparation
First of all, make sure that DeepDive has been [installed](http://deepdive.stanford.edu/doc/basics/installation.html).

Next, DeepDive will store all data—input, intermediate, output, etc.—in a relational database;
currently, Postgres, Greenplum, and MySQL are supported, however Greenplum or Postgres are strongly recommended.
To set the location of this database, we need to configure a URL in the `db.url` file, e.g.:

```bash
echo "postgresql://$USER@$HOSTNAME:5432/deepdive_spouse_$USER" >db.url
```

_Note: DeepDive will drop and then create this database if run from scratch—beware of pointing to an existing populated one!_







## 1. Data processing

In this section, we'll generate the traditional inputs of a statistical learning-type problem: candidate objects, represented by a set of features, which we will aim to classify as _actual_ spouse relation mentions or not.

We'll do this in four basic steps:

1. Loading raw input data
2. Adding NLP markups
3. Extracting candidate relation mentions
4. Extracting features for each candidate

### 1.1. Loading raw input data
Our goal, first of all, is to download and load the raw text of the [articles](http://research.signalmedia.co/newsir16/signal-dataset.html)
into an `articles` table in our database.
We create a simple shell script that downloads & outputs the news articles in TSV format.
DeepDive will automatically create the table, execute the script and load the table if we save it as:

```bash
input/articles.tsv.sh
```

The aforementioned script reads a sample of the corpus (provided as lines of JSON objects), and then using the [jq](https://stedolan.github.io/jq/) language extracts the fields `id` (for document id) and `content` from each entry and converts those to TSV format.

Next, we need to declare the schema of this `articles` table in our `app.ddlog` file; we add the following lines:

```ddlog
articles(
    id      text,
    content text
).
```

Next, we compile our application, as we must do whenever we change `app.ddlog`:

```bash
deepdive compile
```

Finally, we tell DeepDive to execute the steps to load the `articles` table:

```bash
deepdive do articles
```

DeepDive will output an execution plan, which will pop up in your default text editor;
save and exit to accept, and DeepDive will run, creating the table and then fetching & loading the data!

<todo>show example tuples with `deepdive query`</todo>

### 1.2. Adding NLP markups
Next, we'll use Stanford's [CoreNLP](http://stanfordnlp.github.io/CoreNLP/) natural language processing (NLP) system to add useful markups and structure to our input data.
This step will split up our articles into sentences, and their component _tokens_ (roughly, the words).
Additionally, we'll also get _lemmas_ (normalized word forms), _part-of-speech (POS) tags_, _named entity recognition (NER) tags_, and a dependency parse of the sentence.
We declare the output schema of this step in our `app.ddlog`:

```ddlog
sentences(
    doc_id         text,
    sentence_index int,
    sentence_text  text,
    tokens         text[],
    lemmas         text[],
    pos_tags       text[],
    ner_tags       text[],
    doc_offsets    int[],
    dep_types      text[],
    dep_tokens     int[]
).
```

<!-- TODO let's drop this unless the key/distributed_by reveal something important
Note that we declare a compound key of `(doc_id, sentence_index)` for each sentence, and that we declare a `distributed_by` attribute, e.g., primarily for Greenplum, using DDlog annotations.
-->

Next we declare a DDlog function which takes in the `doc_id` and `content` for an article, and returns rows conforming to the sentences schema we just declared, using the **user-defined function (UDF)** in `udf/nlp_markup.sh`.
This UDF is a bash script which calls a [wrapper](https://github.com/HazyResearch/bazaar/tree/master/parser) around CoreNLP.

```ddlog
function nlp_markup over (
        doc_id  text,
        content text
    ) returns rows like sentences
    implementation "udf/nlp_markup.sh" handles tsv lines.
```

Finally, we specify that this `nlp_markup` function should be run over each row from `articles`, and the output appended to `sentences`:

```ddlog
sentences += nlp_markup(doc_id, content) :-
    articles(doc_id, content).
```

Again, to execute, we compile and then run:

```bash
deepdive compile
```
```bash
deepdive do sentences
```

<todo>show example tuples with `deepdive query`</todo>

Note that the previous steps—here, loading the articles—will _not_ be re-run unless we specify that they should be, using, e.g.:

```bash
deepdive mark todo articles
```

### 1.3. Extracting candidate relation mentions

#### Mentions of people
Once again we first declare the schema:

```ddlog
person_mention(
    mention_id     text,
    mention_text   text,
    doc_id         text,
    sentence_index int,
    begin_index    int,
    end_index      int
).
```

We will be storing each person as a row referencing a sentence and beginning and ending indexes.
Again we next declare a function which references a UDF, and takes as input the sentence tokens and NER tags:

```ddlog
function map_person_mention over (
        doc_id         text,
        sentence_index int,
        tokens         text[],
        ner_tags       text[]
    ) returns rows like person_mention
    implementation "udf/map_person_mention.py" handles tsv lines.
```

We'll write a simple UDF in Python that will tag spans of contiguous tokens with the NER tag "PERSON" as person mentions (i.e., we'll essentially rely on CoreNLP's NER module).
Note that we've already used a bash script as a UDF, and indeed any programming language can be used (DeepDive will just check the path specified in the top line, e.g., `#!/usr/bin/env python`)/
However DeepDive provides some convenient utilities for Python UDFs which handle all IO encoding/decoding.
To write our UDF, we'll start by specifying that our UDF will handle TSV lines (as specified in the DDlog above);
additionally we'll specify the exact type schema of both input and output, which DeepDive will check for us:

```python
{% include examples/spouse/udf/map_person_mention.py %}
```

Above, we write a simple function which extracts and tags all subsequences of tokens having the NER tag "PERSON".
Note that the `extract` function must be a generator, i.e., use a `yield` statement to return output rows.

Finally, we specify that the function will be applied to rows from the `sentences` table and append to the `person_mention` table:

```ddlog
person_mention += map_person_mention(
    doc_id, sentence_index, tokens, ner_tags
) :-
    sentences(doc_id, sentence_index, _, tokens, _, _, ner_tags, _, _, _).
```

Again, to run, just compile & execute as in previous steps:

```bash
deepdive compile && deepdive do person_mention
```

<todo>show example tuples with `deepdive query`</todo>


#### Mentions of spouses (pairs of people)
Next, we'll take all pairs of **non-overlapping person mentions that co-occur in a sentence with less than 5 people total,** and consider these as the set of potential ('candidate') spouse mentions.
We thus filter out sentences with large numbers of people for the purposes of this tutorial; however these could be included if desired.
Again, to start, we declare the schema for our `spouse_candidate` table—here just the two names, and the two person_mention IDs referred to:

```ddlog
spouse_candidate(
    p1_id   text,
    p1_name text,
    p2_id   text,
    p2_name text
).
```

Next, for this operation we don't use any UDF script, instead relying entirely on DDlog operations.
We simply construct a table of person counts, and then do a join with our filtering conditions; in DDlog this looks like:

```ddlog
num_people(doc_id, sentence_index, COUNT(p)) :-
    person_mention(p, _, doc_id, sentence_index, _, _).

spouse_candidate(p1, p1_name, p2, p2_name) :-
    num_people(same_doc, same_sentence, num_p),
    person_mention(p1, p1_name, same_doc, same_sentence, p1_begin, _),
    person_mention(p2, p2_name, same_doc, same_sentence, p2_begin, _),
    num_p < 5,
    p1_name != p2_name,
    p1_begin != p2_begin.
```

Again, to run, just compile & execute as in previous steps.

```bash
deepdive compile && deepdive do spouse_candidate
```

<todo>show example tuples with `deepdive query`</todo>


### 1.4. Extracting features for each candidate
Finally, we will extract a set of **features** for each candidate:

```ddlog
spouse_feature(
    p1_id   text,
    p2_id   text,
    feature text
).
```

The goal here is to represent each spouse candidate mention by a set of attributes or **_features_** which capture at least the key aspects of the mention, and then let a machine learning model learn how much each feature is correlated with our decision variable ('is this a spouse mention?').
For those who have worked with machine learning systems before, note that we are using a sparse storage representation-
you could think of a spouse candidate `(p1_id, p2_id)` as being represented by a vector of length `L = count(distinct(feature))`, consisting of all zeros except for at the indexes specified by the rows with key `(p1_id, p2_id)`.

DeepDive includes an automatic feature generation library, DDlib, which we will use here.
Although many state-of-the-art [applications](http://deepdive.stanford.edu/doc/showcase/apps.html) have been built using purely DDlib-generated features, others can be used and/or added as well.  To use DDlib, we create a list of `ddlib.Word` objects, two `ddlib.Span` objects, and then use the function `get_generic_features_relation`:

```python
{% include examples/spouse/udf/extract_spouse_features.py %}
```

Note that getting the input for this UDF requires joining the `person_mention` and `sentences` tables:

```ddlog
function extract_spouse_features over (
        p1_id          text,
        p2_id          text,
        p1_begin_index int,
        p1_end_index   int,
        p2_begin_index int,
        p2_end_index   int,
        doc_id         text,
        sent_index     int,
        tokens         text[],
        lemmas         text[],
        pos_tags       text[],
        ner_tags       text[],
        dep_types      text[],
        dep_tokens     int[]
    ) returns rows like spouse_feature
    implementation "udf/extract_spouse_features.py" handles tsv lines.

spouse_feature += extract_spouse_features(
    p1_id, p2_id, p1_begin_index, p1_end_index, p2_begin_index, p2_end_index,
    doc_id, sent_index, tokens, lemmas, pos_tags, ner_tags, dep_types, dep_tokens
) :-
    person_mention(p1_id, _, doc_id, sent_index, p1_begin_index, p1_end_index),
    person_mention(p2_id, _, doc_id, sent_index, p2_begin_index, p2_end_index),
    sentences(doc_id, sent_index, _, tokens, lemmas, pos_tags, ner_tags, _, dep_types, dep_tokens).
```

Again, to run, just compile & execute as in previous steps.

```bash
deepdive compile && deepdive do spouse_feature
```

<todo>show example tuples with `deepdive query`</todo>

Now we have generated what looks more like the standard input to a machine learning problem—a set of objects, represented by sets of features, which we want to classify (here, as true or false mentions of a spousal relation).
However, we **don't have any supervised labels** (i.e., a set of correct answers) for a machine learning algorithm to learn from!
In most real world applications, a sufficiently large set of supervised labels is _not_ in fact available.
With DeepDive, we take the approach sometimes referred to as _distant supervision_ or _data programming_, where we instead generate a **noisy set of labels using a mix of mappings from secondary datasets & other heuristic rules**.






## 2. Distant supervision with data & rules
In this section, we'll use _distant supervision_ (or '_data programming_') to provide a noisy set of labels to supervise our candidate relation mentions, based on which we can train a machine learning model.

We'll describe two basic categories of approaches:

1. Mapping from secondary data for distant supervision
2. Using heuristic rules for distant supervision

Finally, we'll describe a simple majority-vote approach to resolving multiple labels per example, which can be implemented within DDlog.

### 2.1. Mapping from secondary data for distant supervision
First, we'll try using an external structured dataset of known married couples, from [DBpedia](http://wiki.dbpedia.org/), to distantly supervise our dataset.
We'll download the relevant data, and then map it to our spouse candidate mentions.

#### Extracting & downloading the DBpedia data
Our goal is to first extract a collection of known married couples from DBpedia and then load this into the `spouses_dbpedia` table in our database.
To extract known married couples, we used the DBpedia dump present in [Google's BigQuery platform](https://bigquery.cloud.google.com).
First we extracted the URI, name and spouse information from the DBpedia `person` table records in BigQuery for which the field `name` is not NULL. We used the following query:

```sql
SELECT URI,name, spouse
FROM [fh-bigquery:dbpedia.person]
where name <> "NULL"
```

We stored the result of the above query in a local project table `dbpedia.validnames` and perform a self-join to obtain the pairs of married couples.

```sql
SELECT t1.name, t2.name
FROM [dbpedia.validnames] AS t1
JOIN EACH [dbpedia.validnames] AS t2
ON t1.spouse = t2.URI
```

The output of the above query was stored in a new table named `dbpedia.spouseraw`. Finally, we used the following query to remove symmetric duplicates.

```sql
SELECT p1, p2
FROM (SELECT t1_name as p1, t2_name as p2 FROM [dbpedia.spouseraw]),
     (SELECT t2_name as p1, t1_name as p2 FROM [dbpedia.spouseraw])
WHERE p1 < p2
```

The output of this query was stored in a local file `spousesraw.csv`. The file contained duplicate rows (BigQuery does not support `distinct`) and noisy rows where the name field contained a string where the given name family name and multiple aliases where concatenated and reported in a string including the characters `{` and `}`. Using the Unix commands `sed`, `sort` and `uniq` we first removed the lines containing characters `{` and `}` and then duplicate entries. This resulted in an input file `spouses_dbpedia.csv` containing 6,126 entries of married couples.

#### Loading DBpedia data to database
We compress and store `spouses_dbpedia.csv` under the path:

```bash
input/spouses_dbpedia.csv.bz2
```

Notice that for DeepDive to load the data to the corresponding database table the name of the input data again has to be stored in the directory `input/` and has the same name as the target database table. To load the data we execute the command:

```bash
deepdive do spouses_dbpedia
```

<todo>show example tuples with `deepdive query`</todo>


#### Supervising spouse candidates with DBpedia data
First we'll declare a new table where we'll store the labels (referring to the spouse candidate mentions), with an integer value (`True=1, False=-1`) and a description (`rule_id`):

```ddlog
spouse_label(
    p1_id   text,
    p2_id   text,
    label   int,
    rule_id text
).
```

Next we'll implement a simple distant supervision rule which labels any spouse mention candidate with a pair of names appearing in DBpedia as true:

```ddlog
# distant supervision using data from DBpedia
spouse_label(p1,p2, 1, "from_dbpedia") :-
    spouse_candidate(p1, p1_name, p2, p2_name), spouses_dbpedia(n1, n2),
    [ lower(n1) = lower(p1_name), lower(n2) = lower(p2_name) ;
      lower(n2) = lower(p1_name), lower(n1) = lower(p2_name) ].
```

It should be noted that there are many clear ways in which this rule could be improved (fuzzy matching, more restrictive conditions, etc.), but this serves as an example of one major type of distant supervision rule.

### 2.2. Using heuristic rules for distant supervision
We can also create a supervision rule which does not rely on any secondary structured dataset like DBpedia, but instead just uses some heuristic.
We set up a DDlog function, `supervise`, which uses a UDF containing several heuristic rules over the mention and sentence attributes:

```ddlog
function supervise over (
        p1_id text, p1_begin int, p1_end int,
        p2_id text, p2_begin int, p2_end int,
        doc_id         text,
        sentence_index int,
        sentence_text  text,
        tokens         text[],
        lemmas         text[],
        pos_tags       text[],
        ner_tags       text[],
        dep_types      text[],
        dep_tokens     int[]
    ) returns (
        p1_id text, p2_id text, label int, rule_id text
    )
    implementation "udf/supervise_spouse.py" handles tsv lines.

spouse_label += supervise(
    p1_id, p1_begin, p1_end,
    p2_id, p2_begin, p2_end,
    doc_id, sentence_index, sentence_text,
    tokens, lemmas, pos_tags, ner_tags, dep_types, dep_token_indexes
) :-
    spouse_candidate(p1_id, _, p2_id, _),
    person_mention(p1_id, p1_text, doc_id, sentence_index, p1_begin, p1_end),
    person_mention(p2_id, p2_text,      _,              _, p2_begin, p2_end),
    sentences(
        doc_id, sentence_index, sentence_text,
        tokens, lemmas, pos_tags, ner_tags, _, dep_types, dep_token_indexes
    ).
```

The Python UDF contains several heuristic rules:

* Candidates with person mentions that are too far apart in the sentence are marked as false.
* Candidates with person mentions that have another person in between are marked as false.
* Candidates with person mentions that have words like "wife" or "husband" in between are marked as true.
* Candidates with person mentions that have "and" in between and "married" after are marked as true.
* Candidates with person mentions that have familial relation words in between are marked as false.

```python
{% include examples/spouse/udf/supervise_spouse.py %}
```

Note that the rough theory behind this approach is that we don't need high-quality, e.g., hand-labeled supervision to learn a high quality model;
instead, using statistical learning, we can in fact recover high-quality models from a large set of low-quality—or **_noisy_**—labels.

### 2.3. Resolving multiple labels per example with majority vote
Finally, we implement a very simple majority vote procedure, all in DDlog, for resolving scenarios where a single spouse candidate mention has multiple conflicting labels.
First, we sum the labels (which are all -1, 0, or 1):

```ddlog
spouse_label_resolved(p1_id, p2_id, SUM(vote)) :- spouse_label(p1_id, p2_id, vote, rule_id).
```

Then, we simply threshold, and add these labels to our decision variable table `has_spouse` (see next section for details here):

```ddlog
has_spouse(p1_id, p2_id) = if l > 0 then TRUE
                      else if l < 0 then FALSE
                      else NULL end :- spouse_label_resolved(p1_id, p2_id, l).
```

We additionally make sure that all spouse candidate mentions _not_ labeled by a rule are also included in this table:

```ddlog
has_spouse(p1, p2) = NULL :- spouse_candidate(p1, _, p2, _).
```

Once again, to execute all of the above, just run the following command:

```bash
deepdive compile && deepdive do has_spouse
```

Recall that `deepdive do` will execute all upstream tasks as well, so this will execute all of the previous steps!


<todo>show example tuples with `deepdive query`</todo>





## 3. Learning & inference: model specification
Now, we need to specify the actual model that DeepDive will perform learning and inference over.
At a high level, this boils down to specifying three things:

1. What are the _variables_ of interest, that we want DeepDive to predict for us?

2. What are the _features_ for each of these variables?

3. What are the _connections_ between the variables?

One we have specified the model in this way, DeepDive will _learn_ the parameters of the model (the weights of the features and potentially of the connections between variables), and then perform _statistical inference_ over the learned model to determine the most likely values of the variables of interest.

For more advanced users: we are specifying a _factor graph_ where the features are unary factors, and then using SGD and Gibbs Sampling for learning and inference; further technical detail is available [here](#).

### 3.1. Specifying prediction variables
In our case, we have one variable to predict per spouse candidate mention, namely, **is this mention actually indicating a spousal relation or not?**
In other words, we want DeepDive to predict the value of a boolean variable for each spouse candidate mention, indicating whether it is true or not.
We specify this in `app.ddlog` as follows:

```ddlog
has_spouse?(
    p1_id text,
    p2_id text
).
```

DeepDive will predict not only the value of these variables, but also the marginal probabilities, i.e., the amount of confidence DeepDive has for each individual prediction.

### 3.2. Specifying features
Next, we indicate (i) that each `has_spouse` variable will be connected to the features of the corresponding `spouse_candidate` row, (ii) that we wish DeepDive to learn the weights of these features from our distantly supervised data, and (iii) that the weight of a specific feature across all instances should be the same, as follows:

```ddlog
@weight(f)
has_spouse(p1_id, p2_id) :-
    spouse_candidate(p1_id, _, p2_id, _),
    spouse_feature(p1_id, p2_id, f).
```

### 3.3. Specifying connections between variables
Finally, we can specify relations between the prediction variables, with either learned or given weights.
Here, we'll specify two such rules, with fixed (given) weights that we specify.
First, we define a _symmetry_ connection, namely specifying that if the model thinks a person mention `p1` and a person mention `p2` indicate a spousal relationship in a sentence, then it should also think that the reverse is true, i.e., that `p2` and `p1` indicate one too:

```ddlog
@weight(3.0)
has_spouse(p1_id, p2_id) => has_spouse(p2_id, p1_id) :-
    spouse_candidate(p1_id, _, p2_id, _).
```

Next, we specify a rule that the model should be strongly biased towards finding one marriage indication per person mention.
We do this inversely, using a negative weight, as follows:

```ddlog
@weight(-1.0)
has_spouse(p1_id, p2_id) => has_spouse(p1_id, p3_id) :-
    spouse_candidate(p1_id, _, p2_id, _),
    spouse_candidate(p1_id, _, p3_id, _).
```


<todo>show learning/inference steps, example results</todo>





## 4. Error analysis & debugging

After finishing a pass of writing and running the DeepDive application, the first thing we want to see is how good the results are.
In this section, we describe how DeepDive's interactive tools can be used for viewing the results as well as error analysis and debugging.


### 4.1. Browsing data with Mindbender

*Mindbender* is the name of the tool that provides an interactive user interface to DeepDive.
It can be used for browsing any data that has been loaded into DeepDive and produced by it.

#### Browsing input corpus

We need to give hints to DeepDive which part of the data we want to browse [using DDlog's annotation](browsing.md#ddlog-annotations-for-browsing).
For example, on the `articles` relation we declared earlier in `app.ddlog`, we can sprinkle some annotations such as `@source`, `@key`, and `@searchable`, as the following.

```ddlog
@source
articles(
    @key
    id text,
    @searchable
    content text
).
```

Next, if we run the following command, DeepDive will create and populate a search index according to these hints.

```bash
mindbender search update
```

To access the populated search index through a web browser, run:

```bash
mindbender search gui
```

Then, point your browser to the URL that appears after the command (typically <http://localhost:8000>) to see a view that looks like the following:

![Screenshot of the search interface showing input corpus](images/browsing_corpus.png)


#### Browsing result data

To browse the results, we can add annotations to the derived relations and how they relate to their source relations.
For example, the `@extraction` and `@references` annotations in the following DDlog declaration tells DeepDive that the variable relation `has_spouse` is derived from pairs of `person_mention`.

```ddlog
@extraction
has_spouse?(
    @key
    @references(relation="person_mention", column="mention_id", alias="p1")
    p1_id text,
    @key
    @references(relation="person_mention", column="mention_id", alias="p2")
    p2_id text
).
```

The relation `person_mention` as well as the relations it references should have similar annotations (see the [complete `app.ddlog` code](https://github.com/HazyResearch/deepdive/blob/master/examples/spouse/app.ddlog) for full detail).

Then repeating the commands to update the search index and load the user interface will allow us to browse the expected marginal probabilities of `has_spouse` as well.

![Screenshot of the search interface showing results](images/browsing_results.png)


#### Customizing how data is presented

<!-- TODO describe presentation annotations once it's ready -->

In fact, the screenshots above are showing the data presented using a [carefully prepared set of templates under `mindbender/search-templates/`](https://github.com/HazyResearch/deepdive/tree/master/examples/spouse/mindbender/search-template/).
In these AngularJS templates, virtually anything you can program in HTML/CSS/JavaScript/CoffeeScript can be added to present the data that is ideal for human consumption, e.g., highlighted text spans rather than token indexes.
Please see the [documentation about customizing the presentation](browsing.md#customizing-presentation) for further detail.


### 4.2. Estimating precision with Mindtagger

*Mindtagger*, which is part of the Mindbender tool suite, assists data labeling tags to quickly assess the precision and/or recall of the extraction.
We show how Mindtagger helps us perform a labeling task to estimate the precision of the extraction.
The necessary set of files shown below already exist [in the example under `labeling/has_spouse-precision/`](https://github.com/HazyResearch/deepdive/tree/master/examples/spouse/labeling/has_spouse-precision/).

<!-- TODO describe how a task can be created from the search interface instead, once it's ready -->

#### Preparing a data labeling task

First, we can take a random sample of 100 examples from `has_spouse` relation whose expectation is higher than or equal to a 0.9 threshold, and store them in a file called `has_spouse.csv`.

<!-- TODO use deepdive-query instead once it allows the @expectation syntax to grab such field for variable relations -->

```bash
deepdive sql eval "
{% include examples/spouse/labeling/has_spouse-precision/sample-has_spouse.sql %}
" format=csv header=1 >labeling/has_spouse-precision/has_spouse.csv
```

We also prepare the `mindtagger.conf` and `template.html` files under `labeling/has_spouse-precision/` that look like the following:

```hocon
{% include examples/spouse/labeling/has_spouse-precision/mindtagger.conf %}
```

```html
{% include examples/spouse/labeling/has_spouse-precision/template.html %}
```

#### Labeling data with Mindtagger

Mindtagger can then be started for the task using the following command:

```bash
mindbender tagger labeling/has_spouse-precision/mindtagger.conf
```

Then, point your browser to the URL that appears after the command (typically <http://localhost:8000>) to see a dedicated user interface for labeling data that looks like the following:

![Screenshot of the labeling interface showing the sampled data](images/mindtagger_screenshot.png)

We can quickly label the sampled 100 examples using the intuitive user interface with buttons for correct/incorrect tags that also supports keyboard shortcuts for entering labels and moving between items.
(Press the <kbd>?</kbd> key to view all supported keys.)
How many were labeled correct as well as other tags are shown in the "Tags" dropdown at the top right corner as shown below.

![Screenshot of the labeling interface showing tag statistics](images/mindtagger_screenshot_tags.png)

The collected tags can also be exported in various format for post-processing.

![Screenshot of the labeling interface for exporting tags](images/mindtagger_screenshot_export.png)

For further detail, see the [documentation about labeling data](labeling.md).


### 4.3. Monitoring statistics with Dashboard

<todo>write</todo>
