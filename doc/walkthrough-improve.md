---
layout: default
---

# Improving the Results

This page is the next section after [Example Application: A Mention-Level Extraction System](walkthrough-mention.html).

<a id="improve" href="#"> </a>

## Contents

- [How to examine Results](#examine)
- [Reduce sparsity](#sparsity)
- [Use strong indicators rather than bag of words](#strong_words)
- [Add a domain-specific (symmetry) rule](#symmetry)
- [Tune sampler parameter](#tune_sampler)
- [Using dependency path](#dependency)
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
l = len(words_between)
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
lemma_between = ddlib.tokens_between_spans(obj["lemma"], span1, span2)
words_between = words[left_idx:right_idx]
married_words = ['marry', 'widow', 'wife', 'fiancee', 'spouse']
non_married_words = ['father', 'mother', 'brother', 'sister', 'son']
# Make sure the distance between mention pairs is not too long
if len(words_between) <= 10:        
  for mw in married_words + non_married_words:
    if mw in lemma_between: 
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
  WHERE s.sentence_id = hsi.sentence_id and expectation > 0.9
  ORDER BY random() LIMIT 10;
"
{% endhighlight %}

Results looks like:

     sentence_id |           description            | is_true | expectation |  sentence
    -------------+----------------------------------+---------+-------------+---------------------------
           79231 | Biden-Jill                       |         |       0.995 | Biden 's wife , Jill , had come on stage to hug her husband and tell him about a `` very special surprise guest . ''
           68539 | Su Hua-Newton                    |         |       0.994 | In 1977 , Newton and his wife , Su Hua , founded Newton Vineyard in St. Helena .
           67114 | John McCain-Cindy McCain         | t       |       0.998 | GOP front-runner John McCain 's wife , Cindy McCain -- who has been the focus of fashionistas ' public commentary for her carefully coifed hair and perfect makeup -- made news by noting that she is , indeed , `` proud of my country . ''
           61883 | Apatow-Paul Rudd                 |         |       0.982 | She lives with her sister -LRB- Apatow 's real-life wife , Leslie Mann -RRB- and brother-in-law -LRB- Paul Rudd -RRB- and kids -LRB- Apatow 's real-life kids -RRB- , in a bickering but basically happy fog of suburbia .
           62721 | Samantha Morton-Deborah          |         |       0.996 | The domestic scenes are less impressive , with Samantha Morton typically bedraggled as wife Deborah , and Alexandra Maria Lara unable to flesh out the underwritten part of girlfriend Annik .
           72676 | Cindy-McCain                     |         |       0.992 | McCain 's wife , Cindy , an heiress to a beer distributorship fortune , is believed to be worth tens of millions of dollars , but the exact amount is unclear .
           46783 | Rostropovich-Galina Vishnevskaya |         |       0.993 | Rostropovich and his wife , Galina Vishnevskaya , provided shelter at their country home to Solzhenitsyn and his family shortly before the writer was expelled from the Soviet Union in 1974 .
           75847 | CINDY MCCAIN-Cindy               |         |       0.983 | The New York Times said in editorials for Monday , May 19 : CINDY MCCAIN 'S MONEY Sen. John McCain 's wife , Cindy , has decided not to release her tax returns -- not now and not in the future .
           72416 | Yasser Arafat-Suha Arafat        | t       |       0.943 | She attended an event in Ramallah on the West Bank at which the wife of Yasser Arafat , Suha Arafat , accused Israel of poisoning Palestinian women and children with toxic gases .
           66817 | John Edwards-Elizabeth           |         |       0.993 | John Edwards 's wife , Elizabeth , and Barack Obama 's wife , Michelle , have been stalwarts on the trail .
    (10 rows)

And the feature weights look like:

                     description                  |       weight
    ----------------------------------------------+--------------------
     f_has_spouse_features-important_word=marry   |    2.7278060779874
     f_has_spouse_features-important_word=wife    |   2.30546918096395
     f_has_spouse_features-important_word=fiancee |   1.93391840800263
     f_has_spouse_features-important_word=widow   |   1.58736658337015
     f_has_spouse_symmetry-                       |   1.10917376733438
     f_has_spouse_features-important_word=spouse  |  0.663277095834514
     f_has_spouse_features-few_words_between      |  0.440539900127551
     f_has_spouse_features-many_words_between     | -0.154142245562194
     f_has_spouse_features-last_word_matches      | -0.202544977430098
     f_has_spouse_features-important_word=mother  | -0.371781328596943
     f_has_spouse_features-other_person_between   | -0.496146339718239
     f_has_spouse_features-important_word=sister  | -0.642663000298456
     f_has_spouse_features-important_word=brother | -0.735863925859598
     f_has_spouse_features-important_word=father  |  -2.22903288408165
    (14 rows)

We can see that the results have been improved quite a bit, though there might still be some errors. To make further improvements, we can conduct more error analysis, and make use of better features such as dependency paths. Moreover, performing entity linking and [looking for entity-level relations](walkthrough.html#entity_level) is necessary for a better KBC application.

Now if you want, you can look at the [Extras page](walkthrough-extras.html) which explained how to prepare data tables, use pipelines, use NLP extractors, or get example extractor inputs.

You can also [go back to the tutorial](walkthrough.html#entity_level).


