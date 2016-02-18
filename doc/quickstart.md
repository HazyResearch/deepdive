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

Now, let's see what DeepDive can do for us. We grab a copy of [the spouse example app explained in the tutorial](example-spouse.md).
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

You can find a more datasets under `input/`. You can also [download the full corpus][corpus], but let's proceed with the smallest one that has 100 sampled articles:

```bash
ln -s articles-100.tsv.bz2 input/articles.tsv.bz2
```
```bash
deepdive do articles
```

This will load the input data into the database.
*Note that everytime you use the `deepdive do` command, it opens a list of commands to be run in your text editor. You have to confirm it by saving and quiting the editor.*

Here're a few lines from an example article in the input corpus that has been loaded.

```bash
deepdive query '?- articles("36349778-9942-475d-bdf2-23b7372911c1", content).' format=csv | tail -n +15 | head -5
```
```
How could anybody with a brain have ever supported such a facile, drug-addled ignoramus as Bachman, who recently said the only way to avoid war with Iran is to bomb it? But it happens every election, with homicidal crazies urging war on the world to counter false flag threats that have been cynically engineered by the CIA and Mossad. Thanks to a war-mongering media run by the same money men who sell the weapons and commit the colossal crimes, Americans never learn.

But just as scary as the bizarre also-rans of history is the list of the people who have actually won presidential elections. Consider in recent years the immoral Bill Clinton, who gave America’s jobs away; George Bush the Younger, who was so dim-witted he had to be left out of the plot to wreck the World Trade Center lest through his stupidity he accidentally revealed the secret Jewish plan; and Barack Obama, who has no verifiable history at all except a suspicious trail of dead homosexual friends and a wife with a male physiognomy.

So I guess we shouldn’t be too surprised at the sudden emergence of megalomillionaire Donald Trump as the leader of this year’s pack. He has done what other early leaders have always done, told people exactly what they want to hear.

```

<!--
<todo>find a better doc_id to show here, that contains well known people in the same sentence</todo>
-->

### 2. Process input

This app [adds some useful NLP markups](example-spouse.md#1-2-adding-nlp-markups) to the English text using [Stanford CoreNLP](http://stanfordnlp.github.io/CoreNLP/).
Based on the marked up *named entity recognition* (NER) tags, it can tell which parts of the text mention people's names.
All pairs of names appearing in the same sentence are [considered as *candidates* for correct mentions of married couples' names](example-spouse.md#1-3-extracting-candidate-relation-mentions).

```bash
deepdive do sentences
```

After running the NLP markup process, we can see the tokens and NER tags for the example article we saw earlier.

```bash
deepdive query '?- sentences("36349778-9942-475d-bdf2-23b7372911c1", _, _, tokens, _, _, ner_tags, _, _, _).' format=csv | grep PERSON | head -5
```
```
"{Lying,hypocrites,beg,for,votes,from,the,misinformed,and,unenlightened,By,John,Kaminski,http://renegadetribune.com/author/kaminski/,http://therebel.is/kaminski,Anyone,who,runs,for,president,of,the,United,States,must,supervise,the,robbery,and,murder,of,innocent,countries,"","",promise,to,maintain,the,unjust,slave,system,of,the,international,bankers,"","",and,lie,about,everything,that,pertains,to,the,safety,of,the,American,people,"","",because,there,is,no,safety,for,anyone,in,these,desperate,times,as,long,as,Jewish,controlled,puppets,continue,to,wreak,havoc,on,the,entire,world,.}","{O,O,O,O,O,O,O,O,O,O,O,PERSON,PERSON,O,O,O,O,O,O,O,O,O,LOCATION,LOCATION,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,MISC,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,MISC,O,O,O,O,O,O,O,O,O,O,O}"
"{Among,the,hundreds,of,delusional,demagogues,who,have,run,for,president,over,the,years,"","",the,most,pathetic,scene,I,ever,witnessed,personally,was,in,the,2012,race,when,a,large,crowd,of,white-haired,retirees,gathered,at,Sarasota,airport,to,enthusiastically,welcome,Minnesota,Rep.,Michelle,Bachman,"","",a,pro-Israel,shill,who,was,later,ridiculed,for,both,her,use,of,anti-depressants,and,her,unflinching,support,for,the,international,bankers,who,have,deliberately,sabotaged,the,American,economy,for,more,than,a,hundred,years,.}","{O,O,O,O,O,O,O,O,O,O,O,O,DURATION,DURATION,O,O,O,O,O,O,O,O,O,O,O,O,DATE,O,O,O,O,O,O,O,O,O,O,LOCATION,O,O,O,O,LOCATION,O,PERSON,PERSON,O,O,MISC,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,MISC,O,O,O,O,O,DURATION,DURATION,O}"
"{How,could,anybody,with,a,brain,have,ever,supported,such,a,facile,"","",drug-addled,ignoramus,as,Bachman,"","",who,recently,said,the,only,way,to,avoid,war,with,Iran,is,to,bomb,it,?}","{O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,PERSON,O,O,DATE,O,O,O,O,O,O,O,O,LOCATION,O,O,O,O,O}"
"{Consider,in,recent,years,the,immoral,Bill,Clinton,"","",who,gave,America,'s,jobs,away,;,George,Bush,the,Younger,"","",who,was,so,dim-witted,he,had,to,be,left,out,of,the,plot,to,wreck,the,World,Trade,Center,lest,through,his,stupidity,he,accidentally,revealed,the,secret,Jewish,plan,;,and,Barack,Obama,"","",who,has,no,verifiable,history,at,all,except,a,suspicious,trail,of,dead,homosexual,friends,and,a,wife,with,a,male,physiognomy,So,I,guess,we,should,n't,be,too,surprised,at,the,sudden,emergence,of,megalomillionaire,Donald,Trump,as,the,leader,of,this,year,'s,pack,.}","{O,O,DURATION,DURATION,O,O,PERSON,PERSON,O,O,O,LOCATION,O,O,O,O,PERSON,PERSON,PERSON,PERSON,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,ORGANIZATION,ORGANIZATION,ORGANIZATION,O,O,O,O,O,O,O,O,O,MISC,O,O,O,PERSON,PERSON,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,PERSON,PERSON,O,O,O,O,DATE,DATE,O,O,O}"
```


We can continue running the processes until all candidates of spousal mentions are mapped, and see the pairs of names from the example article.

```bash
deepdive do spouse_candidate
```
```bash
deepdive query 'name1, name2 ?-
    spouse_candidate(p1, name1, p2, name2),
    person_mention(p1, _, "36349778-9942-475d-bdf2-23b7372911c1", _, _, _).
  '
```
```
          name1          |      name2
-------------------------+-----------------
 Bill Clinton            | Donald Trump
 George Bush the Younger | Bill Clinton
 George Bush the Younger | Barack Obama
 George Bush the Younger | Donald Trump
 Barack Obama            | Bill Clinton
 Barack Obama            | Donald Trump
 Bernie Sanders          | Hillary Clinton
(7 rows)
```

For supervised machine learning, the app continues with [extracting *features*](example-spouse.md#1-4-extracting-features-for-each-candidate) from the context of those candidates and [creating a training set](example-spouse.md#3-learning-amp-inference-model-specification) programmatically by finding promising positive and negative examples using [*distant supervision*](distant_supervision.md).

### 3. Run the model

Using the processed data, the app constructs a [statistical inference model](inference.md) to predict whether a mention is a correct mention of spouses or not, estimates the parameters (i.e., learns the weights) of the model, and computes their *marginal probabilities*.

```bash
deepdive do probabilities
```

As a result, DeepDive gives the expectation (probability) of every variable being true.
Here are the probabilities computed for the pairs of names from the example article we saw earlier:

```bash
deepdive sql "
    SELECT p1.mention_text, p2.mention_text, expectation
    FROM has_spouse_label_inference i, person_mention p1, person_mention p2
    WHERE p1_id LIKE '36349778-9942-475d-bdf2-23b7372911c1%'
      AND p1_id = p1.mention_id AND p2_id = p2.mention_id
  "
```
<!-- TODO switch to DDlog once it gets access to inference results -->
```
      mention_text       |  mention_text   | expectation
-------------------------+-----------------+-------------
 Barack Obama            | Bill Clinton    |       0.002
 Barack Obama            | Bill Clinton    |       0.001
 George Bush the Younger | Bill Clinton    |       0.385
 George Bush the Younger | Barack Obama    |       0.002
 George Bush the Younger | Barack Obama    |           0
 Barack Obama            | Donald Trump    |       0.001
 Barack Obama            | Donald Trump    |           0
 George Bush the Younger | Donald Trump    |           0
 George Bush the Younger | Donald Trump    |           0
 Bill Clinton            | Donald Trump    |           0
 Bill Clinton            | Donald Trump    |           0
 Bernie Sanders          | Hillary Clinton |       0.002
(12 rows)
```

DeepDive provides [a suite of tools and guidelines](development-cycle.md#3-evaluate-amp-debug) to work with the data produced by the application.
For instance, below is a screenshot of an automatic interactive search interface DeepDive provides for [browsing the processed data with predicted results](browsing.md).

![Screenshot of the search interface provided by Mindbender](images/browsing_results.png)



## Next steps

* For more details about the spouse example we just ran here, continue reading [the tutorial](example-spouse.md).

* Other parts of the documentation will help you pick up more [background knowledge](index.md#background-reading) and learn more about [how DeepDive applications are developed](development-cycle.md).

Reading them will prepare you to write your own DeepDive application that can shed light on some dark data and unlock knowledge from it!
