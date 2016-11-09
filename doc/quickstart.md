---
layout: default
title: DeepDive Quick Start
---

# DeepDive Quick Start

DeepDive helps you extract structured knowledge from less-structured data with statistical inference without having to write any sophisticated machine learning code.
Here we show how you can quickly install and run your first DeepDive application.


## Launching or Installing DeepDive

### Quick launching

First, you can quickly [launch DeepDive](installation.md) with minimal installation using [Docker](https://docker.io) by running the following command:.

```bash
bash <(curl -fsSL git.io/getdeepdive)
```

Then selecting the `deepdive_docker_sandbox` option:

```
### DeepDive installer for Mac
1) deepdive                   5) jupyter_notebook
2) deepdive_docker_sandbox    6) postgres
3) deepdive_example_notebook  7) run_deepdive_tests
4) deepdive_from_release      8) spouse_example
# Install what (enter to repeat options, a to see all, q to quit, or a number)? 2
```

Now, point your web browser to [a terminal](http://0.0.0.0:8888/terminals/1) with shell access to an environment where DeepDive is installed.
You will find our examples included there as well.

```bash
cd deepdive-examples/spouse
```

You can also see a notebook version of [the spouse example tutorial](example-spouse.md) if you point your browser to the [tutorial notebook](http://0.0.0.0:8888/notebooks/deepdive-examples/spouse/DeepDive%20Tutorial%20-%20Extracting%20mentions%20of%20spouses%20from%20the%20news.ipynb).


### Quick installation

If you cannot or do not want to use Docker for any reason,
you can quickly [install DeepDive](installation.md) by selecting the `deepdive` option:

```
### DeepDive installer for Mac
1) deepdive                   5) jupyter_notebook
2) deepdive_docker_sandbox    6) postgres
3) deepdive_example_notebook  7) run_deepdive_tests
4) deepdive_from_release      8) spouse_example
# Install what (enter to repeat options, a to see all, q to quit, or a number)? 1
```

While the sandbox provides you with a database, you are on your own with this option.
You need to have a database instance to run any DeepDive application.
You can select `postgres` from DeepDive's installer to install it and spin up an instance on you machine, or just run the following command:

```bash
bash <(curl -fsSL git.io/getdeepdive) postgres
```

Alternatively, if you have access to a database server, you can configure how to access it as a URL in [the application's `db.url` file](deepdiveapp.md#db-url).



## Running your first DeepDive app

Now, let's see what DeepDive can do for us.
We grab a copy of [the spouse example app explained in the tutorial](example-spouse.md).
This app extracts mentions of spouses from [a corpus of news articles][corpus].

(If you launched DeepDive's Docker image, then you can skip this downloading step as it's already included under `deepdive-examples/spouse/`.)

[corpus]: http://research.signalmedia.co/newsir16/signal-dataset.html "The Signal Media One-Million News Articles Dataset"

```bash
bash <(curl -fsSL git.io/getdeepdive) spouse_example
```

This will download a copy of [the example app's code and data from GitHub](../examples/spouse/) to a folder whose name begins with `spouse_example-`.
So, let's move into it:

```bash
cd spouse_example-*
```

Then, check if we have everything there:

```bash
ls -F
```
```
app.ddlog  db.url  deepdive.conf  input/  labeling/  mindbender/  README.md  udf/
```

### 1. Load input
First, you have to compile the DeepDive application using the following command:

```bash
deepdive compile
```
Once it has compiled with no error, you can run the following ```deepdive``` commands.

You can find some of our sampled datasets under `input/`.
You can also [download the full corpus][corpus], but let's proceed with the one that has 1000 sampled articles. Run the following command to load the sampled articles into DeepDive:
```bash
deepdive load articles input/articles-1000.tsv.bz2
```
*Note that everytime you use the `deepdive do` command, it opens a list of commands to be run in your text editor. You have to confirm it by saving and quiting the editor.*

Here are a few lines from an example article in the input corpus that has been loaded.

```bash
deepdive query '?- articles("5beb863f-26b1-4c2f-ba64-0c3e93e72162", content).' format=csv | grep -v '^$' | tail -n +16 | head
```
```
8:30 a.m.
Raeann Meier and Mary Darnell are among the lucky ones to land tickets for Thursday's papal mass at the Basilica of the National Shrine of the Immaculate Conception.
Meier, who's from Round Hill, Virginia, won a pair of tickets in her church lottery and is bringing fellow parishioner Darnell.
Meier says of Francis: ""There is just no pope like this one."" She says ""Jesus hung out with the dregs — the tax collectors, the prostitutes"" and ""that's the way this pope is.""
---
7:50 a.m.
An elaborate welcoming ceremony full of American pomp and pageantry awaits Pope Francis when he goes to the White House.
The pope is scheduled to arrive by motorcade at about 9 a.m., his car pulling slowly up the South Lawn driveway to a red carpet, where President Barack Obama and his wife, Michelle, will be waiting to greet him.
In front of an estimated 15,000 people who were invited by the White House to witness the historic moment, Obama will then lead Francis to a dais decked out with even more red carpet and red, white and blue bunting, and ringed by military color guards. The Vatican and American national anthems will play. Obama will deliver a welcome address to the pope, followed by the pope's address.
Francis will also receive a thunderous 21-gun salute.
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
deepdive query '?- sentences("5beb863f-26b1-4c2f-ba64-0c3e93e72162", _, _, tokens, _, _, ner_tags, _, _, _).' format=csv | grep PERSON | tail
```
```
"{An,elaborate,welcoming,ceremony,full,of,American,pomp,and,pageantry,awaits,Pope,Francis,when,he,goes,to,the,White,House,.}","{O,O,O,O,O,O,MISC,O,O,O,O,PERSON,PERSON,O,O,O,O,O,ORGANIZATION,ORGANIZATION,O}"
"{The,pope,is,scheduled,to,arrive,by,motorcade,at,about,9,a.m.,"","",his,car,pulling,slowly,up,the,South,Lawn,driveway,to,a,red,carpet,"","",where,President,Barack,Obama,and,his,wife,"","",Michelle,"","",will,be,waiting,to,greet,him,.}","{O,O,O,O,O,O,O,O,O,TIME,TIME,TIME,O,O,O,O,O,O,O,LOCATION,LOCATION,O,O,O,O,O,O,O,O,PERSON,PERSON,O,O,O,O,PERSON,O,O,O,O,O,O,O,O}"
"{In,front,of,an,estimated,""15,000"",people,who,were,invited,by,the,White,House,to,witness,the,historic,moment,"","",Obama,will,then,lead,Francis,to,a,dais,decked,out,with,even,more,red,carpet,and,red,"","",white,and,blue,bunting,"","",and,ringed,by,military,color,guards,.}","{O,O,O,O,O,NUMBER,O,O,O,O,O,O,ORGANIZATION,ORGANIZATION,O,O,O,O,O,O,PERSON,O,O,O,PERSON,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O}"
"{Obama,will,deliver,a,welcome,address,to,the,pope,"","",followed,by,the,pope,'s,address,.}","{PERSON,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O}"
"{Francis,will,also,receive,a,thunderous,21-gun,salute,.}","{PERSON,O,O,O,O,O,O,O,O}"
"{People,hoping,to,catch,a,glimpse,of,Pope,Francis,during,a,late,morning,parade,are,lining,up,for,a,coveted,spot,along,the,route,.}","{O,O,O,O,O,O,O,O,PERSON,O,O,TIME,TIME,O,O,O,O,O,O,O,O,O,O,O,O}"
"{As,a,head,of,state,"","",Pope,Francis,officially,is,in,the,U.S.,on,what,'s,known,as,a,``,state,visit,.,''}","{O,O,O,O,O,O,O,PERSON,O,O,O,O,LOCATION,O,O,O,O,O,O,O,O,O,O,O}"
"{For,one,thing,"","",President,Barack,Obama,and,Francis,will,not,review,the,troops,"","",as,presidents,do,with,other,visiting,leaders,.}","{O,NUMBER,O,O,O,PERSON,PERSON,O,PERSON,O,O,O,O,O,O,O,O,O,O,O,O,O,O}"
"{Nor,will,Francis,return,to,the,White,House,in,the,evening,as,the,guest,at,a,lavish,state,dinner,"","",one,of,the,highlights,of,most,state,visits,.}","{O,O,PERSON,O,O,O,LOCATION,LOCATION,O,O,TIME,O,O,O,O,O,O,O,O,O,NUMBER,O,O,O,O,O,O,O,O}"
"{That,'s,largely,because,of,Francis,',busy,schedule,.}","{O,O,O,O,O,PERSON,O,O,O,O}"
```


We can continue running the processes until all candidates of spousal mentions are mapped, and see the pairs of names from the example article.

```bash
deepdive do spouse_candidate
```
```bash
deepdive query 'name1, name2 ?-
    spouse_candidate(p1, name1, p2, name2),
    person_mention(p1, _, "5beb863f-26b1-4c2f-ba64-0c3e93e72162", _, _, _).
  '
```
```
    name1     |    name2
--------------+--------------
 Raeann Meier | Mary Darnell
 Meier        | Darnell
 Meier        | Francis
 Barack Obama | Francis
 Francis      | Obama
 Barack Obama | Michelle
 Obama        | Francis
 Barack Obama | Francis
(8 rows)
```

For supervised machine learning, the app continues with [extracting *features*](example-spouse.md#1-4-extracting-features-for-each-candidate) from the context of those candidates and [creating a training set](example-spouse.md#3-learning-and-inference-model-specification) programmatically by finding promising positive and negative examples using [*distant supervision*](distant_supervision.md).

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
    FROM has_spouse_inference i, person_mention p1, person_mention p2
    WHERE p1_id LIKE '5beb863f-26b1-4c2f-ba64-0c3e93e72162%'
      AND p1_id = p1.mention_id AND p2_id = p2.mention_id
  "
```
<!-- TODO switch to DDlog once it gets access to inference results -->
```
 mention_text | mention_text | expectation
--------------+--------------+-------------
 Raeann Meier | Mary Darnell |       0.129
 Meier        | Darnell      |           0
 Meier        | Darnell      |           0
 Meier        | Francis      |       0.009
 Barack Obama | Francis      |       0.002
 Francis      | Obama        |       0.011
 Barack Obama | Michelle     |       0.648
 Barack Obama | Michelle     |       0.598
 Obama        | Francis      |       0.014
 Barack Obama | Francis      |       0.017
(10 rows)
```

DeepDive provides [a suite of tools and guidelines](development-cycle.md#3-evaluate-debug) to work with the data produced by the application.
For instance, below is a screenshot of an automatic interactive search interface DeepDive provides for [browsing the processed data with predicted results](browsing.md).

![Screenshot of the search interface provided by Mindbender](images/browsing_results.png)



## Next steps

* For more details about the spouse example we just ran here, continue reading [the tutorial](example-spouse.md).

* Other parts of the documentation will help you pick up more [background knowledge](index.md#background-reading) and learn more about [how DeepDive applications are developed](development-cycle.md).

Reading them will prepare you to write your own DeepDive application that can shed light on some dark data and unlock knowledge from it!
