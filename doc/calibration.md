---
layout: default
---

# Calibration

Writing factors and inference rules using DeepDive is an iterative process. After the DeepDive pipeline is finished, you should inspect the results, improve your extractors, and modify inference rules. To make this process easier, DeepDive writes calibration data for each variable. [Refer to the calibration guide for more information](/doc/general/calibration.html).

### Defining holdout

To get the most out of calibration, you should specify a holdout fraction for your training data. The holdout will be used to evaluate the predictions DeepDive makes. By default, no holdout is used.

    deepdive.calibration: {
      holdout_fraction: 0.25
    }

The system assigns holdout variables at random. That is, for each evidence variable, it flips a coin to decide whether to put the variable into the holdout or not.

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

Deepdive also generates a calibration plot for each of the variables you defined in your schema:

<br/>
![]({{site.baseurl}}/images/calibration_example.png)






