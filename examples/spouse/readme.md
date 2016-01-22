# Extracting Spouse Relationships from News Articles: An end-to-end illustrative Deepdive Example

We'll walk through this tutorial as if the reader were starting from scratch,
however the full finalized example application is in this directory, which the reader should refer to throughout the tutorial.

## Loading the input data
First, we need to fetch the raw data which we'll be using.
In general for a DeepDive application one might want to get:
  1. **Input data**: In this case, we'll be extracting our spouse relation mentions from a [set of raw text news articles](http://research.signalmedia.co/newsir16/signal-dataset.html)
  2. **Distant supervision data:** Instead of using directly supervised training data, we'll use a technique known as [distant supervision](http://deepdive.stanford.edu/doc/general/distant_supervision.html).  Specifically here, we'll download a structured dataset of known married couples from [Freebase](https://www.freebase.com/), and use this to programatically supervise examples in our input data.

### Basic setup
First of all, make sure that DeepDive has been [installed](http://deepdive.stanford.edu/doc/basics/installation.html)!

Next, DeepDive will store all data- input, intermediate, output, etc- in a relational database;
currently, Postgres, Greenplum, and MySQL are supported, however Greenplum or Postgres are strongly recommended.
To set the location of this database, we need to define a `db.url` file, e.g.:
```bash
echo "[postgresql|greenplum]://[USER]@[HOST]:[PORT]/deepdive_spouse" > db.url
```
_Note: DeepDive will drop and then create this database if run from scratch- beware of pointing to an existing populated one!_

### Loading the news articles
Our goal, first of all, is to download and load the raw text of the articles into an `articles` table in our database.
We create a simple shell script that downloads & outputs the news articles in `tsv` format.
DeepDive will automatically create the table, execute the script and load the table if we save it as:
```bash
input/articles.tsv.sh
```

Next, we need to define the schema of this `articles` table in our `app.ddlog` file; we add the following lines:
```
# example DeepDive application for finding spouse relationships in news articles
@source
articles(
    @key
    id text,
    @searchable
    content text
).
```
Note that we add decorators to denote that this is an input _source_,
that the article id is a _key_ for the relation, and that the raw content should be _searchable_ in our analysis tools; more on this later on!

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



**TODO: Describe how to load any data from freebase.**


##Corpus Exploration with Mindbender (Optional)

1. Describe the annotations added in the basic articles relation.
2. Describe how to setup mindbender.
3. Describe which commands to run to get the mindbender environment up and running.

##Generating Sentence NLP-Markups

##Generating Candidates

##Generating Features


