---
layout: default
---

# Example Application: Improving the Results

This page is the next section after [Example Application: A Mention-Level Extraction System](walkthrough-mention.html).

<a id="improve" href="#"> </a>

## Contents

- [How to examine Results](#examine)
- [Reduce sparsity](#sparsity)
- [Use strong indicators rather than bag of words](#strong_words)
- [Add a domain-specific (symmetry) rule](#symmetry)
- [Tune sampler parameter](#tune_sampler)
- [Getting improved results](#improved_results)

Other sections:

- [Walkthrough](walkthrough.html)
- [A Mention-Level Extraction System](walkthrough-mention.html)
- [Extras: preprocessing, NLP, pipelines, debugging extractor](walkthrough-extras.html)


<a id="examine" href="#"> </a>

### How to Examine Results

We first talk about several common methods in DeepDive to examine results. DeepDive generates [calibration plots](general/calibration.html) for all variables defined in the schema to help with debugging. Let's take a look at the generated calibration plot, written to the file outputted in the summary report above. It should look something like this:

![Calibration]({{site.baseurl}}/assets/walkthrough_has_spouse_is_true.png)

The calibration plot contains useful information that help you to improve the quality of your predictions. For actionable advice about interpreting calibration plots, refer to the [calibration guide](general/calibration.html). 

Often, it is also useful to look at the *weights* that were learned for features or rules. You can do this by looking at the `mapped_inference_results_weights` table in the database. Type in following command to select features with highest weight (positive features):

{% highlight bash %}
psql -d deepdive_spouse -c "
  SELECT description, weight
  FROM dd_inference_result_variables_mapped_weights
  ORDER BY weight DESC
  LIMIT 5;
"
{% endhighlight %}

                    description                 |      weight
    --------------------------------------------+------------------
     f_has_spouse_features-word_between=widower | 4.67905738706231
     f_has_spouse_features-word_between=D-N.Y.  | 3.83342838871529
     f_has_spouse_features-word_between=wife    | 3.16971392862251
     f_has_spouse_features-word_between=going   | 2.93433356931802
     f_has_spouse_features-word_between=spouse  | 2.89820086224187
    (5 rows)

Type in following command to select top negative features:

{% highlight bash %}
psql -d deepdive_spouse -c "
  SELECT description, weight
  FROM dd_inference_result_variables_mapped_weights
  ORDER BY weight ASC
  LIMIT 5;
"
{% endhighlight %}

                      description                  |      weight
    -----------------------------------------------+-------------------
     f_has_spouse_features-word_between=son        | -3.71654440726897
     f_has_spouse_features-word_between=Mann       | -3.29400179392337
     f_has_spouse_features-word_between=addressing | -3.16472057957028
     f_has_spouse_features-word_between=campaigned | -2.84878387581674
     f_has_spouse_features-word_between=were       | -2.59003087248654
    (5 rows)


Note that each execution may learn different weights, and these lists can look different. Generally, we might see that most weights make sense while some don't.

<!-- You can further improve the prediction by different ways. There are many possible strategies including:

- Making use of co-reference information
- Performing entity linking instead of extraction relations among mentions in the text
- Adding more inference rules that encode your domain knowledge
- Adding more (or better) positive or negative training examples
- Adding more (or better) features

For the second point: our goal in this tutorial is get an initial
application up and running. There are a couple of problems with the
approach above which are worth drawing attention to: If two separate
sentences mention the fact that Barack Obama and Michelle Obama are in a
`has_spouse` relationship, then our approach does not know that they
refer to the same fact. In other words, we ignore the fact that "Barack
Obama" and "Michelle Obama" in both of these sentence refer to the same
entity in the real world. We also don't recognize *coreference* of two
mentions. That is, we don't know that "Barack Obama" and "Obama"
probably refer to the same person. 
 -->

<!-- We will address these issues in the [advanced part of the tutorial](walkthrough2.html). -->


<!-- Here we can see that the word phrase "and-former-President" in between the two person names has a rather high weight. This seems strange, since this phrase is not an indicator of a marriage relationship. One way to improve our predictions would be to add more negative evidence that would lower the weight of that feature.
 -->


<!-- TODO!!!! -->

<a id="sparsity" href="#"> </a>

### Reduce Sparsity

We notice that feature `num_words_between` suffers from sparsity issues and would cause overfitting. For example, there should be roughly no difference between having 20 and 21 words between two entity mentions. Change *"Feature 2"* for `ext_has_spouse_features.py`:

{% highlight python %}
# Feature 2: Number of words between the two phrases
# Intuition: if they are close by, the link may be stronger.
l = len(words_between.elements)
if l < 5: features.add("few_words_between")
else: features.add("many_words_between")
{% endhighlight %}


<a id="strong_words" href="#"> </a>

### Use strong indicators rather than bag of words

The "bag of words" is a pretty weak feature. Our next improvement is using strong indicators rather than bag of words. We check the lemma of words between two mentions if they are strong indicators of spouse or non-spouse relationships, such as "marry" or "widow", or "father" "mother".

Start by modifying `application.conf` to select `lemma` as input query to `ext_has_spouse_features`:

      ext_has_spouse_features {
        input: """
          SELECT  sentences.words,
                  lemma,                   # Add this line
                  has_spouse.relation_id,
                  p1.start_position  AS  "p1.start_position",
                  p1.length          AS  "p1.length",
                  p2.start_position  AS  "p2.start_position",
                  p2.length          AS  "p2.length"
          ...

Then modify `ext_has_spouse_features.py` by changing *Feature 1* (bag of words) into this feature. We still make use of `ddlib`:

{% highlight python %}
# Feature 1: Find out if a lemma of marry occurs.
# A better feature would ensure this is on the dependency path between the two.
words_between = ddlib.tokens_between_spans(words, span1, span2)
lemma_between = ddlib.tokens_between_spans(obj["lemma"], span1, span2)
married_words = ['marry', 'widow', 'wife', 'fiancee', 'spouse']
non_married_words = ['father', 'mother', 'brother', 'sister', 'son']
# Make sure the distance between mention pairs is not too long
if len(words_between) <= 10:         
  for mw in married_words + non_married_words:
    if mw in lemma_between.elements: 
      features.add("important_word=%s" % mw)
{% endhighlight %}

<!-- {% highlight python %}
# Feature 1: Find out if a lemma of marry occurs.
# A better feature would ensure this is on the dependency path between the two.
left_idx = min(p1_end, p2_end)
right_idx = max(p1_start, p2_start)

lemma_between = obj["lemma"][left_idx:right_idx]
words_between = words[left_idx:right_idx]
married_words = ['marry', 'widow', 'wife', 'fiancee', 'spouse']
non_married_words = ['father', 'mother', 'brother', 'sister']
if len(words_between) <= 10:
  for mw in married_words + non_married_words:
    if mw in lemma_between: 
      features.add("important_word=%s" % mw)
{% endhighlight %} -->

The `married_words` and `non_married_words` list can be obtained through a "snowball-style" feature engineering: if you do not know which words to add, you could run bag of words and check high-weight / low-weight features (via the SQL query), and pick reasonable words to add. 


<a id="symmetry" href="#"> </a>

### Add a domain-specific rule

let's try to incorporate a bit of domain knowledge into our model. For example, we know that has_spouse is symmetric. That means, if Barack Obama is married to Michelle Obama, then Michelle Obama is married to Barack Obama, and vice versa. (`Marry(A,B) <-> Marry(B,A)`) We can encode this knowledge in a second inference rule:

    inference.factors {

      # ...(other inference rules)

      f_has_spouse_symmetry {
        input_query: """
          SELECT r1.is_true AS "has_spouse.r1.is_true",
                 r2.is_true AS "has_spouse.r2.is_true",
                 r1.id      AS "has_spouse.r1.id",
                 r2.id      AS "has_spouse.r2.id"
          FROM has_spouse r1,
               has_spouse r2
          WHERE r1.person1_id = r2.person2_id
            AND r1.person2_id = r2.person1_id
          """
        function: "Equal(has_spouse.r1.is_true, has_spouse.r2.is_true)"
        weight: "?"
      }

    }

There are many [other kinds of factor functions](inference_rule_functions.html) you could use to encode domain knowledge. 


<a id="tune_sampler" href="#"> </a>

### Tune sampler parameter

We can further tune sampler parameters to obtain better results. Refer to [performance tuning guide](performance.html) and [sampler guide](sampler.html) for tuning sampler parameters.

Add into `deepdive` block of `application.conf`:

    sampler.sampler_args: "-l 5000 -d 0.99 -s 1 -i 1000 --alpha 0.01"

This would force sampler to learn and sample with more iterations and a slower decay of stepsize.

<a id="improved_results" href="#"> </a>

### Getting improved results

After adding above modifications to extractors and inference rules, let's rerun the system:

{% highlight bash %}
./run.sh

psql -d deepdive_spouse -c "
  SELECT s.sentence_id, description, is_true, expectation, s.sentence
  FROM has_spouse_is_true_inference hsi, sentences s
  WHERE s.sentence_id = hsi.sentence_id and expectation > 0.95
  ORDER BY random() LIMIT 10;
"
{% endhighlight %}

Results looks like:

     sentence_id |        description        | is_true | expectation | sentence
    -------------+---------------------------+---------+-------------+------------------------------------
           40575 | St. Vincent-Carole Rome   |         |       0.985 | Gov. Charlie Crist made an appearance at St. Vincent with his fiancee , Carole Rome , and handed out plates stacked with turkey , potatoes and stuffing .
           21353 | Judd Apatow-Leslie Mann   | t       |       0.958 | TRUE LOVE , FAMILY VALUES ALIVE IN ` KNOCKED UP ' IN -LCB- LSQUO -RCB- KNOCKED UP , ' FAMILY VALUES GET A TWIST MISMATCHPRODUCESLAUGHS AND LOVE SURPRISEFAMILY LOVE Knocked Up Written and directed by : Judd Apatow Starring : Seth Rogen , Katherine He
    igl , Paul Rudd , Leslie Mann Running time : 129 minutes Rated : R -LRB- Sexual content , drug use , language -RRB- `` The 40-Year-Old Virgin '' and his new `` Knocked Up '' make writer - director Judd Apatow today 's preeminent creator of low-concept sex comedies .
           18008 | Diane Lane-Josh Brolin    | t       |       0.968 | And does anyone know why Josh Brolin and Diane Lane went separate ways as soon as they entered the Chateau foyer ?
           23391 | Sally-Salino              |         |       0.995 | The breakup left both parents unable to properly care for the boys , ages 2 1/2 and 1 , and the specter of having the children placed in foster care prompted the Salino and his wife , Sally , to move the boys into their house .
            4416 | Codey-Corzine             |         |       0.991 | Codey said he and his wife hoped to visit Corzine by Thursday , depending on his progress .
           28418 | Tommy Lee-Pamela Anderson | t       |       0.957 | Rock solid Kid says he 's still standing tall despite all the drama in his life By Adam Graham Detroit News Pop Music Writer Fistfights with Tommy Lee , multiple marriages to -LRB- and a subsequent divorce from -RRB- Pamela Anderson : They say no pr
    ess is bad press , and Kid Rock is hoping the exposure pays off when his new album , `` Rock N Roll Jesus , '' is released Tuesday .
           25097 | Ben-Anne Meara            |         |       0.994 | In addition to son Ben , Jerry Stiller has had his real-life wife Anne Meara on the show frequently .
           23196 | Ashton Kutcher-Demi Moore | t       |       0.974 | `` It 's not quite Demi Moore and Bruce Willis going on vacation with Ashton Kutcher , '' she said , `` but that 's an ideal , is n't it ? ''
           21447 | Michelle-Wright           |         |       0.996 | And so , with those remarks , a tightly knit relationship finally unraveled -- Wright had married Obama and his wife , Michelle , and baptized their children .
           20401 | Goold-Lady Macbeth        |         |       0.978 | He even had notes for Macbeth , played by Patrick Stewart , and for Lady Macbeth , Kate Fleetwood , who happens to be married to Goold .
    (10 rows)

Let's look at the calibration plot:

![Calibration]({{site.baseurl}}/assets/walkthrough_has_spouse_is_true_improved.png)

Let's examine the learned weights again. Type in following command to select features with highest weight:


{% highlight bash %}
psql -d deepdive_spouse -c "
  SELECT description, weight
  FROM dd_inference_result_variables_mapped_weights
  ORDER BY weight DESC
  LIMIT 5;
"
{% endhighlight %}

                    description                 |      weight
    --------------------------------------------+------------------
     f_has_spouse_features-important_word=widow | 2.07552405288369
     f_has_spouse_features-important_word=wife  | 2.02799834116762
     f_has_spouse_features-important_word=marry |  1.4300430882985
     f_has_spouse_features-few_words_between    | 1.20873177437353
     f_has_spouse_symmetry-                     | 1.08043386981184
    (5 rows)

Type in following command to select top negative features:

{% highlight bash %}
psql -d deepdive_spouse -c "
  SELECT description, weight
  FROM dd_inference_result_variables_mapped_weights
  ORDER BY weight ASC
  LIMIT 5;
"
{% endhighlight %}

                       description                   |       weight
    -------------------------------------------------+--------------------
     f_has_spouse_features-important_word=son        |  -2.59031900689398
     f_has_spouse_features-important_word=father     |  -2.14158159298961
     f_has_spouse_features-potential_last_name_match |  -1.71109060211146
     f_has_spouse_features-important_word=brother    | -0.552613937997688
     f_has_spouse_features-important_word=mother     | -0.364695428177156
    (5 rows)

We can see that the results have been improved quite a bit, but there are still some errors. 

From the calibration plot we can tell that there are not enough features, especially negative features. We can continue "snowball sampling" on bag of words to obtain more negative features, or use better features such as dependency paths. We can also add more negative examples by distant supervision, or adding other domain-specific rules. To make further improvements, it is important to conduct error analysis.

Moreover, performing entity linking and [looking for entity-level relations](walkthrough.html#entity_level) is necessary for a better KBC application.

Now if you want, you can look at the [Extras page](walkthrough-extras.html) which explained how to prepare data tables, use pipelines, use NLP extractors, or get example extractor inputs.

You can also [go back to the tutorial](walkthrough.html#entity_level).


