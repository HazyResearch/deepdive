---
layout: default
title: "Tutorial: Simple DeepDive Example"
---

<br><todo>

- Modernize the example code
- and migrate content from examples/smoke/README.md

</todo>

#Simple DeepDive Example: Cancer/Smoke/Friends Example

This simple toy example uses a classical Markov Logic Network example to show probabilistic interence and factor graphs functionalities of DeepDive. You can read more about [probabilistic inference and factor graphs](http://deepdive.stanford.edu/doc/general/inference.html) in our detailed documentation.

###Inference rules

The objectives in this example are to infer whether a person smokes, and whether a person has cancer with a some probability using the factor function `imply(A,B)`. Here, `imply(A,B)` means that if `A` is true, then `B` is also true.

We introduce two rules:
        1. If person A smokes, then A might have a cancer.
        2. If two people A and B are friends and A smokes, then B might also smoke.

The following are examples of inference rules for the two forementioned rules (specified in the deepdive.conf file):

<todo>ddlog</todo>

```bash
deepdive {
        inference.factors {
                # rule number 1
                smokes_cancer {
                        input_query: """
                                SELECT person_has_cancer.id as "person_has_cancer.id",
                                       person_smokes.id as "person_smokes.id",
                                       person_smokes.smokes as "person_smokes.smokes",
                                       person_has_cancer.has_cancer as "person_has_cancer.has_cancer"
                                  FROM person_has_cancer, person_smokes
                                 WHERE person_has_cancer.person_id = person_smokes.person_id
                        """
                        function: "Imply(person_smokes.smokes, person_has_cancer.has_cancer)"
                        weight: 0.5
                }

                # rule number 2
                friends_smoke {
                        input_query: """
                                SELECT p1.id AS "person_smokes.p1.id",
                                       p2.id AS "person_smokes.p2.id",
                                       p1.smokes AS "person_smokes.p1.smokes",
                                       p2.smokes AS "person_smokes.p2.smokes"
                                  FROM friends INNER JOIN person_smokes AS p1
                                   ON (friends.person_id = p1.person_id) INNER JOIN person_smokes AS p2
                                   ON (friends.friend_id = p2.person_id)
                        """
                        function: "Imply(person_smokes.p1.smokes, person_smokes.p2.smokes)"
                        weight: 0.4
                }
        }
}

```
### <a name="Setting Up" href="#"></a> Setting Up

Before running the example, please check that DeepDive has been properly [installed](http://deepdive.stanford.edu/doc/basics/installation.html) and the necessary files (app.ddlog, db.url, and deepdive.conf) and directory (input/) that are associated with this example are stored in the current working directory. Input directory should have data files (friends.tsv, person_has_cancer.tsv, person_smokes.tsv, and person.tsv). In order to use DeepDive a database instance must be running to accept requests, and the database location must be specified in the db.url. You can refer to the detailed [tutorial](example-spouse.md) to setup the environemnt.

### <a name="Running" href="#"></a> Running

Now you are ready to run the example. First, you have to compile the code using the following command.
```bash
deepdive compile
```
Once it has compiled with no error, you can run the following command to see the list of deepdive targets.
```bash
deedive do
```
To run the entire pipeline you can run the following command.
```bash
deepdive run
```
This will display a plan for deepdive to run your pipeline. To start the pipeline, exit the editor with *:wq* command.

### <a name="Results" href="#"></a> Results

Once the pipeline has completed running, you can view the results in the database using sql or ddlog command. The entire database should look like this:

```bash
                                            List of relations
 Schema |                         Name                         | Type  | Owner |    Size    | Description
--------+------------------------------------------------------+-------+-------+------------+-------------
 public | dd_graph_variables_holdout                           | table | user | 0 bytes    |
 public | dd_graph_variables_observation                       | table | user | 0 bytes    |
 public | dd_graph_weights                                     | view  | user | 0 bytes    |
 public | dd_inference_result_variables                        | table | user | 8192 bytes |
 public | dd_factors_inf_imply_person_smokes_person_has_cancer | table | user | 8192 bytes |
 public | dd_factors_inf_imply_person_smokes_person_smokes     | table | user | 8192 bytes |
 public | dd_weights_inf_imply_person_smokes_person_has_cancer | table | user | 16 kB      |
 public | dd_weights_inf_imply_person_smokes_person_smokes     | table | user | 16 kB      |
 public | friends                                              | table | user | 8192 bytes |
 public | person                                               | table | user | 16 kB      |
 public | person_has_cancer                                    | table | user | 8192 bytes |
 public | person_has_cancer_label_calibration                  | view  | user | 0 bytes    |
 public | person_has_cancer_label_inference                    | view  | user | 0 bytes    |
 public | person_smokes                                        | table | user | 8192 bytes |
 public | person_smokes_label_calibration                      | view  | user | 0 bytes    |
 public | person_smokes_label_inference                        | view  | user | 0 bytes    |
(16 rows)
```
Tables person, friends, person_has_cancer, and person_smokes told the input data we specified in the input/ directory. To see what DeepDive infered from our data you can look at person_smokes_label_inference and  person_has_cancer_label_inference. The two views should look like the following:

```bash
#person_smokes_label_inference
 person_id | id | label | category | expectation
-----------+----+-------+----------+-------------
         4 |  9 |       |        1 |       0.643
         2 |  7 |       |        1 |       0.506
         6 | 11 |       |        1 |       0.468
         5 | 10 |       |        1 |       0.451
(4 rows)

#person_has_cancer_label_inference
 person_id | id | label | category | expectation
-----------+----+-------+----------+-------------
         3 |  2 |       |        1 |       0.635
         1 |  0 |       |        1 |       0.614
         6 |  5 |       |        1 |        0.57
         2 |  1 |       |        1 |       0.563
         4 |  3 |       |        1 |       0.563
         5 |  4 |       |        1 |       0.551
(6 rows)
```

The `id` column is for internal usage and can be ignored by the user and `person_id` is the user defined ID in the input data. You can see that DeepDive uses the given data and inference rules to predict whether a person smokes and whether a person has cancer with some expectation.


