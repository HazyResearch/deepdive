---
layout: default
---

# Inference Results and Calibration

Writing factors and inference rules using DeepDive is an iterative process. After the DeepDive pipeline is finished, you should inspect the results, improve your extractors, and modify inference rules. The inference results are stored in the database, in the table named `[variable name]_inference`. DeepDive gives expectation for each variable, which is the most probable value that the variable may take. Also, the learned weights are store in table `dd_inference_result_weights`. To make this process easier, DeepDive writes calibration data for each variable. [Refer to the calibration guide for more information](general/calibration.html).

### Defining holdout

To get the most out of calibration, you should specify a holdout fraction for your training data. The holdout will be used to evaluate the predictions DeepDive makes. By default, no holdout is used.

    calibration: {
      holdout_fraction: 0.25
    }

By default the system assigns holdout variables at random. You can also specify a custom holdout query (see below)


### Custom Holdout

DeepDive allows you to specify a SQL query that defines which variables should be in the holdout. In case you define a custom query to hold out in `holdout_query`, and the `holdout_fraction` setting is ignored. You may define a custom holdout query as follows:

    calibration: {
      holdout_query: "INSERT INTO dd_graph_variables_holdout(variable_id) SELECT id FROM mytable WHERE predicate"
    }

A custom holdout query should insert all variable IDs that are to be held out into the `dd_graph_variables_holdout` table through arbitrary SQL. Such query may look as follows (but could be more complex):

{% highlight sql %}
INSERT INTO dd_graph_variables_holdout(variable_id)
[A SELECT statement that selects the IDs of all variables to be in the holdout]
{% endhighlight %}


### Inspecting Probabilities and Weights

To improve prediction accuracy it is useful to inspect the probabilities for each variable, and the learned weights. DeepDive creates a view called `inference_result_mapped_weights` in the database, which contains the weight names and the learned values sorted by absolute value.

For each variable, DeepDive generates a view called `[variable_name]_inference`, which contains your original data, augmented with a `probability` column, the result of our inference algorithm.



### Calibration Plots

The system generates a calibration data file for each variable defined in the [schema](schema.html). Refer to the log output for the path of these files. By default, they are placed in `target/calibration/[variable_name].tsv`. Each files contains five columns:

    [bucket_from] [bucket_to] [num_predictions] [num_true] [num_false]

DeepDive buckets the inference results into ten buckets from 0.0 to 1.0. Thus, each file will contain ten lines.

  - `num_predictions` is the number of variables in the probability bucket, including both holdout and query variables.
  - `num_true` is the number of variables in the probability bucket for which the holdout value is true. The number should be high for buckets with large probabilities and small for buckets with small probabilities. Only the holdout data is used.
  - `num_false` is the number of variables in the probability bucket for which the holdout value is false. The number should be small for buckets with large probabilities and large for buckets with small probabilities. Only the holdout data is used.

DeepDive also generates a calibration plot for each of the variables you defined in your schema:

<br/>

![]({{site.baseurl}}/assets/walkthrough_has_spouse_is_true.png)






