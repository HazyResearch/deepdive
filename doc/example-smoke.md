---
layout: default
title: "Tutorial: Simple DeepDive Example"
---

#Simple DeepDive Example: Cancer/Smoke/Friends Example

This simple toy example uses a classical Markov Logic Network example to show probabilistic interence and factor graphs functionalities of DeepDive. You can read more about [probabilistic inference and factor graphs](http://deepdive.stanford.edu/doc/general/inference.html) in our detailed documentation.

###Inference rules

The objectives in this example are to predict whether a person smokes, and whether a person has cancer with a some probability using the factor function `imply(A,B)`. Here, `imply(A,B)` means that if `A` is true, then `B` is also true.

We introduce two rules:
        1. If person A smokes, then A might have a cancer.
        2. If two people A and B are friends and A smokes, then B might also smoke.

The following are examples of inference rules for the two forementioned rules (specified in the deepdive.conf file):

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

### <a name="Running the Example" href="#"></a> Running the Example

Before running the example make sure DeepDive has been properly [installed](http://deepdive.stanford.edu/doc/basics/installation.html), and the necessary files (app.ddlog, db.url, and deepdive.conf) and directory (input/) that are associated with this example are stored in the current working directory. Input directory should have data files (friends.tsv  person_has_cancer.tsv  person_smokes.tsv  person.tsv). In order to use DeepDive a database instance must be running to accept requests, and the database location must be specified in the db.url. You can refer to the detailed [walkthrough](http://deepdive.stanford.edu/doc/basics/walkthrough/walkthrough.html) to setup the environemnt.
We first have to compile the code using the following command.
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
