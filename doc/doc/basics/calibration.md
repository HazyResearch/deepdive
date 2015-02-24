---
layout: default
---

# <a name="calibration" href="#"></a> Calibration

One of the most important aspects of DeepDive is its iterative workflow. After
having performed [probabilistic inference](../general/inference.html) using
DeepDive, it is crucial to evaluate the results and act on the feedback that the
system provides to improve the accuracy. DeepDive produces *calibration plots*
to help the user with this task.

### <a name="holdout" href="#"></a> Defining holdout

To get the most out of calibration, the user should specify a *holdout fraction* for
the training data. DeepDive uses the holdout to evaluate its predictions. By
default, no holdout is used.

```bash
calibration: {
  holdout_fraction: 0.25
}
```

By default the system randomly selects which variables belong to the holdout
set. Only variables with evidence can be selected.

####<a name="custom_holdout" href="#"></a> Custom Holdout

DeepDive allows to specify a SQL query that defines which variables should
be in the holdout set. A custom holdout query must insert all variable IDs that
are to be held out into the `dd_graph_variables_holdout` table through arbitrary
SQL. A custom holdout query can be specified as follows:

```bash
calibration: {
  holdout_query: "INSERT INTO dd_graph_variables_holdout(variable_id) SELECT id FROM mytable WHERE predicate"
}
```

When a custom holdout query is defined in `holdout_query`, the
`holdout_fraction` setting is ignored. 

### Inspecting probabilities and weights

To improve the prediction accuracy it is useful to inspect the probabilities for
each variable, and the learned factor weights. DeepDive creates a view called
`dd_inference_result_weights_mapping` which contains the factor names and the
learned values sorted by absolute value. The
`dd_inference_result_weights_mapping` view has the following schema:

    View "public.dd_inference_result_weights_mapping"
       Column    |       Type       | Modifiers
    -------------+------------------+-----------
     id          | bigint           |
     isfixed     | integer          |
     initvalue   | real             |
     cardinality | text             |
     description | text             |
     weight      | double precision |


Specification for these fields:

- **id**: the unique identifier for the weight
- **initial_value**: the initial value for the weight
- **is_fixed**: whether the weight is fixed (cannot be changed during learning)
- **cardinality**: the cardinality of this factor. Meaningful for [multinomial factors](chunking.html).
- **description**: description of the weight, composed by [the name of inference rule]-[the specified value of "weight" in inference rule]
- **weight**: the learned weight value

DeepDive also creates a view `dd_feature_statistics` that maps factor names, weights and number of positive / negative examples associated with this factor:

         View "public.dd_feature_statistics"
        Column    |       Type       | Modifiers
    --------------+------------------+-----------
     id           | bigint           |
     isfixed      | integer          |
     initvalue    | real             |
     cardinality  | text             |
     description  | text             |
     weight       | double precision |
     pos_examples | bigint           |
     neg_examples | bigint           |
     queries      | bigint           |

It has all columns from `dd_inference_result_weights_mapping`, and three additional columns:

- **pos_examples**: The number of positive examples associated with this feature.
- **neg_examples**: The number of negative examples associated with this feature.
- **queries**: The number of queries associated with this feature.

Note these columns contain non-NULL values only when the factor is a unary `IsTrue` factor.

This table can be used to diagnose features, e.g. if a feature gets
too high weight, it might because there are not enough negative
examples for this feature, and you may need to add more data or more 
distant supervision rules.

### Calibration data and plots

The system generates a calibration data file for each variable defined in the
[schema](schema.html). The output log contains the path of these files. By
default, they are placed in `target/calibration/[variable_name].tsv`. Each file
contains ten lines with five columns each:

[bucket_from] [bucket_to] [num_predictions] [num_true] [num_false]

DeepDive places the inference results into ten buckets. Each bucket is
associated to a probability interval from 0.0 to 1.0. The meaning of the last
three columns is the:


- `num_predictions` is the number of variables in the probability bucket,
  including both holdout and query variables (unknown variables).
  It can be shown as num_predictions = num_holdout + num_unknown_var.

- `num_true` is the number of holdout variables in the probability bucket with the 
  value of true. The number should be high for buckets with large
  probabilities and small for buckets with small probabilities since the actual value
  of these variables are true and with high probability they should be predicted as true. 
  Not that in this case only the holdout data is used.

- `num_false` is the number of holdout variables in the probability bucket with the 
  value of false. The number should be small for buckets with large
  probabilities and large for buckets with small probabilities since the actual value
  of these variables are false and with low probability they should be predicted as true.
  Not that in this case only the holdout data is used.


DeepDive also generates a calibration plot for each of the variables defined
in schema. The location of the plot is given in the DeepDive output of each run:

13:05:28 [profiler] INFO  calibration plot written to $DEEPDIVE_HOME/out/2014-06-23T130346/calibration/has_spouse.is_true.png [0 ms]
	
DeepDive also prints in its output the commands to generate the calibration
plots, so the user can create the plots manually.

#### Interpreting calibration plots

A typical calibration plot looks as follows:

![]({{site.baseurl}}/images/calibration_example.png)


**The accuracy plot (a)** shows the ratio of correct positive predictions
for each probability bucket. Ideally, the red line should follow the blue line,
representing that the system finds high number of evidence positive predictions for higher probability buckets and for lower probability buckets the system finds less number of evidence positive predictions linearly. Which means for probability bucket of 0 there should be no positive prediction, and for 100% bucket all the predictions should be positive. The accuracy is defined as num_holdout_true / num_holdout_total.

**Plots (b) and (c)** shows the number of total prediction on the test and the
training set, respectively. Ideally these plots should follow a U-curve. That
is, the systems makes many predictions with probability 0 (event that are likely
to be false), and many predictions with probability > 0.9 (events that are
likely to be true). Predictions in the range of 0.4 - 0.6 mean that the system
is not sure, which may indicate that need more features to make predictions for
such events.

Note that plots (a) and (b) can only be generated if a [holdout fraction was
specified in the configuration](#holdout).

#### Acting on calibration data

There could many factors that lead to suboptimal results. Common ones are:

- **Not enough features:** this is particularly common when a lot of
  probability mass falls in the middle buckets (0.4 - 0.6). The system may be unable
  to make predictions about events because the available features are not
  specific-enough for the events. Take a look at variables that were assigned a probability
  in the 0.4 to 0.6 range, inspect them, and come up with specific features that
  would push these variables towards a positive or negative probability.

- **Not enough positive evidence:** without sufficient positive evidence the
  system will be unable to learn weights that push variables towards a high
  probability (or low probability if the variables are negated).
  Having little probability mass on the right side of the graph is often an
  indicator for not having enough positive evidence, or not using features that
  uses the positive evidence effectively.

- **Not enough negative evidence:** without sufficient negative
  evidence, or with negative evidence that is biased, the system will not be
  able to distinguish true events from false events. That is, it will generate
  many *false positives*. In the graph this is often indicated by having little
  probability mass on the left (no U-shape) in plots b) and c), or/and by having
  a low accuracy for high probabilities in plot a). Generating [negative
  evidence](generating_negative_examples.html) can be somewhat of an art.

- **Weight learning does not converge:** when DeepDive is unable to learn
  weights for the inference rules the predicated data will be invalid. Check the
  DeepDive log file for the gradient value at the end of the learning phrase. If
  the value is very large (1000 or more), then it is possible that weight
  learning was not successful. In this case, one may try to increase the number
  of learning iterations, decrease the learning rate, or use a faster decay. On
  the other hand, if results converge too fast to a local optimum, the user can try
  increasing the number of learning iterations, increase the learning rate, or
  using a slower decay. Check the [DimmWitted sampler
  documentation](sampler.html) for more details.

- **Weight learning converges to a local optimum**: the user can try increasing
  the learning rate, or using a slower decay.  Check the [DimmWitted sampler
  documentation](sampler.html) for more details.

### Recall Errors

Recall is the fraction of relevant events that are extracted. In information
extraction applications there are generally two sources of recall error:

- **Events candidates are not recognized in the text.** In this case, no
  variables are created are recall errors and these events do not show up in the
  calibration plots. For example, the system may fail to identify "micro soft" as a
  company name if it is lowercase and misspelled. Such errors are difficult to
  debug, unless there is  a complete database to test against, or the user makes
  a [closed-world
  assumption](http://en.wikipedia.org/wiki/Closed_world_assumption) on the test
  set.

- **Events fall below a confidence cutoff**. Assuming that one is only
  interested in events that have a high probability, then events in the
  mid-range of the calibration plots can be seen as recall errors. For example,
  if one is interested only in company names that are > 90% confident are
  correct, then the company names in the buckets below 0.9 are recall errors.
  Recall can be improved using some of the aforementioned suggestions.

