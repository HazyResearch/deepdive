---
layout: default
---

# Example Application: Improving the Results

This page is the next section after [Example Application: A Mention-Level Extraction System](walkthrough-mention.html).

<a id="improve" href="#"> </a>

### Contents

- [Reduce sparsity](#sparsity)
- [Use strong indicators rather than bag of words](#strong_words)
- [Add a domain-specific (symmetry) rule](#symmetry)
- [Tune sampler parameter](#tune_sampler)
- [Getting improved results](#improved_results)

Other sections:

- [Walkthrough](walkthrough.html)
- [A Mention-Level Extraction System](walkthrough-mention.html)
- [Extras: preprocessing, NLP, pipelines, debugging extractor](walkthrough-extras.html)


<a id="sparsity" href="#"> </a>

### Reduce Sparsity

After [examining the results](walkthrough-mention.html#get_result), we notice that feature `num_words_between` suffers from sparsity issues and would cause overfitting. For example, there should be roughly no difference between having 20 and 21 words between two entity mentions. Change *"Feature 2"* for `ext_has_spouse_features.py`:

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

```bash
ext_has_spouse_features {
  input: """
    SELECT  sentences.words,
            lemma,                   # Add this line
            has_spouse.relation_id,
            p1.start_position  AS  p1_start,
            p1.length          AS  p1_length,
            p2.start_position  AS  p2_start,
            p2.length          AS  p2_length
            """
    # ...
  }
```

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

```bash
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
```

There are many [other kinds of factor functions](inference_rule_functions.html) you could use to encode domain knowledge. 


<a id="tune_sampler" href="#"> </a>

### Tune sampler parameter

We can further tune sampler parameters to obtain better results. Refer to [performance tuning guide](performance.html) and [sampler guide](sampler.html) for tuning sampler parameters.

Add into `deepdive` block of `application.conf`:

```bash
sampler.sampler_args: "-l 5000 -d 0.99 -s 1 -i 1000 --alpha 0.01"
```

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

     sentence_id |       description       | is_true | expectation | sentence 
    -------------+-------------------------+---------+-------------+-------
     95331@69    | Julia Gardiner-B. Tyler |         |           1 | B. Tyler married his second wife , Julia Gardiner , in 1844 in New York City .
     114481@10   | Obama-Michelle          |         |       0.982 | And so , with those remarks , a tightly knit relationship finally came apart -- Wright had married Obama and his wife
     , Michelle , and baptized their children .
     103874@0    | Abigail-John Adams      |         |       0.982 | When John Adams begins acting like a pompous windbag , his wife , Abigail , reproaches him with a single word .
     44768@4     | Wendi-Murdoch           |         |       0.992 | Murdoch 's third wife , Wendi , is a mainland Chinese who once worked for his Hong Kong-based satellite broadcaster ,
     Star TV .
     111325@10   | Julius Rosenberg-Ethel  |         |       0.992 | Sophie Rosenberg thought Mamie Eisenhower could be a `` sympathetic ally '' in saving her son , Julius Rosenberg , an
    d his wife Ethel from execution in 1953 for espionage .
     111325@10   | Ethel-Julius Rosenberg  |         |       0.994 | Sophie Rosenberg thought Mamie Eisenhower could be a `` sympathetic ally '' in saving her son , Julius Rosenberg , an
    d his wife Ethel from execution in 1953 for espionage .
     114424@8    | Obama-Michelle          |         |       0.992 | And so , with those remarks , a tightly knit relationship finally unraveled -- Wright had married Obama and his wife
    , Michelle , and baptized their children .
     1387@16     | Rosalynn-Barbara        |         |       0.978 | Across the nave from the Ford family sat Bush and Laura Bush , and Vice President Dick Cheney , who served Ford as ch
    ief of staff , with his wife , Lynne , several current Cabinet members and three former presidents -- the elder George Bush with his wife , Barbara ; Jimmy Carter and his wife , Rosa
    lynn ; and Bill Clinton and his wife , Sen. Hillary Rodham Clinton , and their daughter Chelsea .
     119377@0    | John McCain-Cindy       |         |       0.992 | Sen. John McCain 's wife , Cindy , abruptly reversed course on Friday and released a summary of her 2006 income tax r
    eturn after weeks of vowing not to do so .
     84632@13    | Cecilia-Sarkozy         |         |       0.998 | Less than two months ago , Sarkozy and his wife , Cecilia , announced their divorce after 11 years of marriage .
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

                     description                  |      weight
    ----------------------------------------------+------------------
     f_has_spouse_features-important_word=wife    | 3.12437525600187
     f_has_spouse_features-important_word=widow   | 2.45652823047255
     f_has_spouse_features-important_word=marry   | 1.85742049055667
     f_has_spouse_features-few_words_between      |  1.6015835203787
     f_has_spouse_features-important_word=fiancee |  1.0439453467637
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
     f_has_spouse_features-important_word=son        |  -2.83397621968201
     f_has_spouse_features-important_word=father     |  -2.76048309192415
     f_has_spouse_features-potential_last_name_match |  -2.34700944702606
     f_has_spouse_features-important_word=brother    |  -2.23063906981248
     f_has_spouse_features-important_word=sister     | -0.523695847147546
    (5 rows)

We can see that the results have been improved quite a bit, but there are still some errors. 

From the calibration plot we can tell that there are not enough features, especially negative features. We can continue "snowball sampling" on bag of words to obtain more negative features, or use better features such as dependency paths. We can also add more negative examples by distant supervision, or adding other domain-specific rules. To make further improvements, it is important to conduct error analysis.

Moreover, performing entity linking and [looking for entity-level relations](walkthrough.html#entity_level) is necessary for a better KBC application.

Now if you want, you can look at the [Extras page](walkthrough-extras.html) which explained how to prepare data tables, use pipelines, use NLP extractors, or get example extractor inputs.

You can also [go back to the tutorial](walkthrough.html#entity_level).


