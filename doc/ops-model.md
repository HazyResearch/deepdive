---
layout: default
title: Learning and inference with the statistical model
---

# Learning and inference with the statistical model

For every DeepDive application, [executing any data processing it defines](ops-execution.md) is ultimately to supply with necessary bits in the construction of the [statistical model declared in DDlog](writing-model-ddlog.md) for [*joint inference*](inference.md).
DeepDive provides several commands to streamline operations on the statistical model, including its creation (*grounding*), parameter estimation (*learning*), and computation of probabilities (*inference*) as well as keeping and reusing the parameters of the model (*weights*).


## Getting the inference result

To simply get the inference results, i.e., the marginal probabilities of the [random variables defined in DDlog](writing-model-ddlog.md#variable-declarations), use the following command:

```bash
deepdive do probabilities
```

This takes care of executing all necessary data processing, then creates a statistical to perform learning and inference, and loads all probabilities of every variable into the database.

### Inspecting the inference result

For viewing the inference result, DeepDive creates a database view that corresponds to each variable relation (using a `_inference` suffix).
For example, the following SQL query can be used for inspecting the probabilities of the variables in relation `has_spouse`:

```bash
deepdive sql "SELECT * FROM has_spouse_inference"
```
<!-- TODO rewrite this in DDlog
```bash
deepdive query '?- @expectation(E) has_spouse(p1,p2)'
```
-->

It shows a table that looks like below where the `expectation` column holds the inferred marginal probability for each variable:

```
                      p1_id                       |                      p2_id                       | expectation
--------------------------------------------------+--------------------------------------------------+-------------
 7b29861d-746b-450e-b9e5-52db4d17b15e_4_5_5       | 7b29861d-746b-450e-b9e5-52db4d17b15e_4_0_0       |       0.988
 ca1debc9-1685-4555-8eaf-1a74e8d10fcc_7_25_25     | ca1debc9-1685-4555-8eaf-1a74e8d10fcc_7_30_31     |       0.972
 34fdb082-a6ef-4b54-bd17-6f8f68acb4a4_15_28_28    | 34fdb082-a6ef-4b54-bd17-6f8f68acb4a4_15_23_23    |       0.968
 7b29861d-746b-450e-b9e5-52db4d17b15e_4_0_0       | 7b29861d-746b-450e-b9e5-52db4d17b15e_4_5_5       |       0.957
 a482785f-7930-427a-931f-851936cd9bb1_2_34_35     | a482785f-7930-427a-931f-851936cd9bb1_2_18_19     |       0.955
 a482785f-7930-427a-931f-851936cd9bb1_2_18_19     | a482785f-7930-427a-931f-851936cd9bb1_2_34_35     |       0.955
 93d8795b-3dc6-43b9-b728-a1d27bd577af_5_7_7       | 93d8795b-3dc6-43b9-b728-a1d27bd577af_5_11_13     |       0.949
 e6530c2c-4a58-4076-93bd-71b64169dad1_2_11_11     | e6530c2c-4a58-4076-93bd-71b64169dad1_2_5_6       |       0.946
 5beb863f-26b1-4c2f-ba64-0c3e93e72162_17_35_35    | 5beb863f-26b1-4c2f-ba64-0c3e93e72162_17_29_30    |       0.944
 93d8795b-3dc6-43b9-b728-a1d27bd577af_3_5_5       | 93d8795b-3dc6-43b9-b728-a1d27bd577af_3_0_0       |        0.94
 216c89a9-2088-4a78-903d-6daa32b1bf41_13_42_43    | 216c89a9-2088-4a78-903d-6daa32b1bf41_13_59_59    |       0.939
 c3eafd8d-76fd-4083-be47-ef5d893aeb9c_2_13_14     | c3eafd8d-76fd-4083-be47-ef5d893aeb9c_2_22_22     |       0.938
 70584b94-57f1-4c8c-8dd7-6ed2afb83031_20_6_6      | 70584b94-57f1-4c8c-8dd7-6ed2afb83031_20_1_2      |       0.938
 ac937bee-ab90-415b-b917-0442b88a9b87_5_7_7       | ac937bee-ab90-415b-b917-0442b88a9b87_5_10_10     |       0.934
 942c1581-bbc0-48ac-bbef-3f0318b95d28_2_35_36     | 942c1581-bbc0-48ac-bbef-3f0318b95d28_2_18_19     |       0.934
 ec0dfe82-30b0-4017-8c33-258e2b2d7e35_36_29_29    | ec0dfe82-30b0-4017-8c33-258e2b2d7e35_36_33_34    |       0.933
 74586dd9-55af-4bb4-9a95-485d5cef20d7_34_8_8      | 74586dd9-55af-4bb4-9a95-485d5cef20d7_34_3_4      |       0.933
 70bebfae-c258-4e9b-8271-90e373cc317e_4_14_14     | 70bebfae-c258-4e9b-8271-90e373cc317e_4_5_5       |       0.933
 ca1debc9-1685-4555-8eaf-1a74e8d10fcc_7_30_31     | ca1debc9-1685-4555-8eaf-1a74e8d10fcc_7_25_25     |       0.928
 ec0dfe82-30b0-4017-8c33-258e2b2d7e35_36_15_15    | ec0dfe82-30b0-4017-8c33-258e2b2d7e35_36_33_34    |       0.927
 f49af9ca-609a-4bdf-baf8-d8ddd6dd4628_4_20_21     | f49af9ca-609a-4bdf-baf8-d8ddd6dd4628_4_15_16     |       0.923
 ec0dfe82-30b0-4017-8c33-258e2b2d7e35_16_9_9      | ec0dfe82-30b0-4017-8c33-258e2b2d7e35_16_4_5      |       0.923
 93d8795b-3dc6-43b9-b728-a1d27bd577af_3_23_23     | 93d8795b-3dc6-43b9-b728-a1d27bd577af_3_0_0       |       0.921
 5530e6a9-2f90-4f5b-bd1b-2d921ef694ef_2_18_18     | 5530e6a9-2f90-4f5b-bd1b-2d921ef694ef_2_10_11     |       0.918
[...]
```

To better understand the inference result for debugging, please refer to the pages about [calibration](calibration.md), [Dashboard](dashboard.md), [labeling](labeling.md), and [browsing data](browsing.md).

The next several sections describe further detail about the different operations on the statistical model supported by DeepDive.



## Grounding the factor graph

The [inference rules written in DDlog](writing-model-ddlog.md#inference-rules) give rise to a data structure called *factor graph* DeepDive uses to perform statistical inference.
*Grounding* is the process of materializing the factor graph as a set of files by laying down all of its [variables and factors in a particular format](factor_graph_schema.md).
This process can be performed using the following command:

```bash
deepdive model ground
```

The above can be viewed as a shorthand for executing the following [built-in processes](ops-execution.md#built-in-data-flow-nodes):

```bash
deepdive redo process/grounding/variable_assign_id process/grounding/combine_factorgraph
```

Grounding generates a set of files for each variable and factor under `run/model/grounding/`.
They are then combined into a unified factor graph under `run/model/factorgraph/` to be easily consumed by the [DimmWitted inference engine](sampler.md) for learning and inference.
For example, below shows a typical list of files holding a grounded factor graph:

```bash
find run/model/grounding -type f
```
```
run/model/grounding/factor/inf_imply_has_spouse_has_spouse/factors.part-1.bin.bz2
run/model/grounding/factor/inf_imply_has_spouse_has_spouse/nedges.part-1
run/model/grounding/factor/inf_imply_has_spouse_has_spouse/nfactors.part-1
run/model/grounding/factor/inf_imply_has_spouse_has_spouse/weights.part-1.bin.bz2
run/model/grounding/factor/inf_imply_has_spouse_has_spouse/weights_count
run/model/grounding/factor/inf_imply_has_spouse_has_spouse/weights_id_begin
run/model/grounding/factor/inf_imply_has_spouse_has_spouse/weights_id_exclude_end
run/model/grounding/factor/inf_istrue_has_spouse/factors.part-1.bin.bz2
run/model/grounding/factor/inf_istrue_has_spouse/nedges.part-1
run/model/grounding/factor/inf_istrue_has_spouse/nfactors.part-1
run/model/grounding/factor/inf_istrue_has_spouse/weights.part-1.bin.bz2
run/model/grounding/factor/inf_istrue_has_spouse/weights_count
run/model/grounding/factor/inf_istrue_has_spouse/weights_id_begin
run/model/grounding/factor/inf_istrue_has_spouse/weights_id_exclude_end
run/model/grounding/factor/weights_count
run/model/grounding/variable/has_spouse/count
run/model/grounding/variable/has_spouse/id_begin
run/model/grounding/variable/has_spouse/id_exclude_end
run/model/grounding/variable/has_spouse/variables.part-1.bin.bz2
run/model/grounding/variable_count
```




## Learning the weights

DeepDive learns the weights of the grounded factor graph, i.e., estimates the maximum likelihood parameters of the statistical model from the variables that were assigned labels via [distant supervision rules written in DDlog](writing-model-ddlog.md#scoping-and-supervision-rules).
DimmWitted inference engine uses *Gibbs sampling* with *stochastic gradient descent* to learn the weights.

The following command performs learning using the grounded factor graph (or grounds a new factor graph if needed):

```bash
deepdive model learn
```

This is equivalent to executing the following targets:

```bash
deepdive redo process/model/learning data/model/weights
```

DimmWitted outputs the learned weights as a text file under `run/model/weights/`.
For convenience, DeepDive loads the learned weights into the database and creates several views for the following target:

```bash
deepdive do data/model/weights
```

This will create a comprehensive view of the weights named `dd_inference_result_weights_mapping`.
The weights corresponding to each inference rule and by their parameter value can be easily accessed using it.
Below shows a few example of learned weights:

```bash
deepdive sql "SELECT * FROM dd_inference_result_weights_mapping"
```

```
    weight    |                      description
--------------+---------------------------------------------------------------
      1.80754 | inf_istrue_has_spouse--INV_NGRAM_1_[wife]
      1.45959 | inf_istrue_has_spouse--NGRAM_1_[wife]
     -1.33618 | inf_istrue_has_spouse--STARTS_WITH_CAPITAL_[True_True]
      1.30884 | inf_istrue_has_spouse--INV_NGRAM_1_[husband]
      1.22097 | inf_istrue_has_spouse--NGRAM_1_[husband]
     -1.00449 | inf_istrue_has_spouse--W_NER_L_1_R_1_[O]_[O]
     -1.00062 | inf_istrue_has_spouse--NGRAM_1_[,]
           -1 | inf_imply_has_spouse_has_spouse-
     -0.94185 | inf_istrue_has_spouse--IS_INVERTED
     -0.91561 | inf_istrue_has_spouse--INV_STARTS_WITH_CAPITAL_[True_True]
     0.896492 | inf_istrue_has_spouse--NGRAM_2_[he wife]
     0.835013 | inf_istrue_has_spouse--INV_NGRAM_1_[he]
    -0.825314 | inf_istrue_has_spouse--NGRAM_1_[and]
     0.805815 | inf_istrue_has_spouse--INV_NGRAM_2_[he wife]
    -0.781846 | inf_istrue_has_spouse--INV_W_NER_L_1_R_1_[O]_[O]
      0.75984 | inf_istrue_has_spouse--NGRAM_1_[he]
     -0.74405 | inf_istrue_has_spouse--INV_NGRAM_1_[and]
     0.701149 | inf_istrue_has_spouse--INV_NGRAM_1_[she]
    -0.645765 | inf_istrue_has_spouse--INV_NGRAM_1_[,]
       0.6105 | inf_istrue_has_spouse--INV_NGRAM_2_[husband ,]
     0.585621 | inf_istrue_has_spouse--INV_NGRAM_2_[she husband]
     0.583075 | inf_istrue_has_spouse--INV_NGRAM_2_[and he]
     0.581042 | inf_istrue_has_spouse--NGRAM_1_[she]
     0.540534 | inf_istrue_has_spouse--NGRAM_2_[husband ,]
[...]
```



## Inference

After learning the weights, DeepDive uses them with the grounded factor graph to compute the marginal probability of every variable.
DimmWitted's high-speed implementation of Gibbs sampling is used for performing a marginal inference by approximately computing the probablities of different values each variable can take over all possible worlds.

```bash
deepdive model infer
```

This is equivalent to executing the following nodes in the data flow:

```bash
deepdive redo process/model/inference data/model/probabilities
```

In fact, because performing inference as a separate process from learning incurs unnecessary overhead of reloading the factor graph into memory again, DimmWitted also performs inference immediately after learning the weights.
Therefore unless previously learned weights are being reused, hence skipping the learning part, the following command that performs just the inference has no effect:

DimmWitted outputs the inferred probabilities as a text file under `run/model/probabilities/`.
As shown in the first section, DeepDive loads the computed probabilities into the database and creates views for convenience.


### Reusing weights

A common use case is to learn the weights from one dataset then performing inference on another, i.e., train model on one dataset and test it on new datasets.

1. Learn the weights from a small dataset.
2. Keep the learned weights.
3. Reuse the kept weights for inference on a larger dataset.

DeepDive provides several commands to support the management and reuse of such learned weights.


#### Keeping learned weights

To keep the currently learned weights for future reuse, say under a name `FOO`, use the following command:

```bash
deepdive model weights keep FOO
```

This dumps the weights from the database into files at `snapshot/model/weights/FOO/` so they can be reused later.
The name `FOO` is optional, and a generated timestamp is used instead when no name is specified.


#### Reusing learned weights

To reuse a previously kept weights, under a name `FOO`, use the following command:

```bash
deepdive model weights reuse FOO
```

This loads the weights at `snapshot/model/weights/FOO/` back to the database, then repeats necessary grounding processes for including the weights into the grounded factor graph.
The name `FOO` is optional, and the most recently kept weights are used when no name is specified.

A subsequent command for performing inference reuses these weights without learning.

```bash
deepdive model infer
```

#### Managing kept weights

DeepDive provides several more commands to manage the kept weights.

To list the names of kept weights, use:

```bash
deepdive model weights list
```

To drop a particular weights, use:

```bash
deepdive model weights drop FOO
```

To clear any previously loaded weights to learn new ones, use:

```bash
deepdive model weights init
```
