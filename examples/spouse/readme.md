# Extracting Spouse Relationships from News Articles: An end-to-end illustrative Deepdive Example

We'll walk through this tutorial as if the reader were starting from scratch,
however the full finalized example application is in this directory, which the reader should refer to throughout the tutorial.

## Loading the input data
First, we need to fetch the raw data which we'll be using.
In general for a DeepDive application one might want to get:
  1. **Input data**: In this case, we'll be extracting our spouse relation mentions from a [set of raw text news articles](http://research.signalmedia.co/newsir16/signal-dataset.html)
  2. **Distant supervision data:** Instead of using directly supervised training data, we'll use a technique known as [distant supervision](http://deepdive.stanford.edu/doc/general/distant_supervision.html).  Specifically here, we'll download a structured dataset of known married couples from [DBpedia](http://wiki.dbpedia.org/), and use this to programatically supervise examples in our input data.

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
The aforementioned script reads a sample of the corpus (provided as lines of json objects), and then using the [jq](https://stedolan.github.io/jq/) language extracts the fields `id` (for docuemnt id) and `content` from each entry and converts those to `tsv` format.

Next, we need to define the schema of this `articles` table in our `app.ddlog` file; we add the following lines:
```
# example DeepDive application for finding spouse relationships in news articles
articles(
    id text,
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


### Generating and Loading the Distant Supervision Data
Our goal is to first extract a collection of known married couples from DBpedia and then load this into the `spouses_dbpedia` table in our database.

#### Extracting the DBpedia Data
To extract known married couples, we used the DBpedia dump present in [Google's BigQuery platform](https://bigquery.cloud.google.com).
First we extracted the URI, name and spouse information from the dbpedia `person` table records in BigQuery for which the field `name` is not NULL. We used the following query:

```
SELECT URI,name, spouse
FROM [fh-bigquery:dbpedia.person]
where name <> "NULL"
```
We stored the result of the above query in a local project table `dbpedia.validnames` and perform a self-join to obtain the pairs of married couples.

```
SELECT t1.name, t2.name
FROM [dbpedia.validnames] AS t1
JOIN EACH [dbpedia.validnames] AS t2
ON t1.spouse = t2.URI
```
The output of the above query was stored in a new table named `dbpedia.spouseraw`. Finally, we used the following query to remove symmetric duplicates.

```
SELECT p1, p2
FROM
  (SELECT t1_name as p1, t2_name as p2
  FROM [dbpedia.spouseraw]),
  (SELECT t2_name as p1, t1_name as p2
  FROM [dbpedia.spouseraw])
WHERE p1 < p2
```
The output of this query was stored in a local file `spousesraw.csv`. The file contained duplicate rows (BigQuery does not support `distinct`) and noisy rows where the name field contained a string where the given name family name and multiple aliases where concatenated and reported in a string including the characters `{` and `}`. Using the unix commands `sed`, `sort` and `uniq` we first removed the lines containing characters `{` and `}` and then duplicate entries. This resulted in an input file `spouses_dbpedia.csv` containing 6,126 entries of married couples.

#### Loading to Database

We compress and store `spouses_dbpedia.csv` under the path:
```bash
input/spouses_dbpedia.csv.bz2
```
Notice that for DeepDive to load the data to the corresponding database table the name of the input data has to be stored in the directory `input/` and has the same name as the target database table. To load the data we execute the command:
```bash
deepdive do spouses_dbpedia
```

##Corpus Exploration with Mindbender (Optional)

This part of the tutorial is optional and focuses on how the user can browse through the input corpus via an automatically generated web-interface. The reader can safelly skip this part.

### DDlog Annotations for Automated Mindtagger
```
@source
articles(
    @key
    id text,
    @searchable
    content text
).
```

### Installing Mindbender
**_TODO: Put in proper way to do this!?_**
Given that `DEEPDIVE_ROOT` is a variable containing the path to the root of the deepdive repo, if you are on linux run:
```bash
wget -O ${DEEPDIVE_ROOT}/dist/stage/bin/mindbender https://github.com/HazyResearch/mindbender/releases/download/v0.2.1/mindbender-v0.2.1-Linux-x86_64.sh
```
for other versions see [the releases page](https://github.com/HazyResearch/mindbender/releases).  Then make sure that this location is on your path:
```bash
export PATH=${DEEPDIVE_ROOT}/dist/stage/bin:$PATH
```

### Running Mindbender
First, generate the input for mindtagger:
```bash
TODO
```
Next, start mindtagger:
```bash
TODO
```


2. Describe how to setup mindbender.
3. Describe which commands to run to get the mindbender environment up and running.

##Generating Sentence NLP-Markups

##Generating Candidates

##Generating Features


