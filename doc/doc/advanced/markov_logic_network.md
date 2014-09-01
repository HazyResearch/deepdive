---
layout: default
---

# Markov Logic Network

This page introduces Deepdive's support for [Markov Logic Network](http://en.wikipedia.org/wiki/Markov_logic_network)(MLN). This piece of work takes the program parser of [Tuffy](http://i.stanford.edu/hazy/hazy/tuffy/) to generate a database containing all atoms in the MLN and a Deepdive program containing the formulas in the MLN as Deepdive inference rules. Then the generated Deepdive program will be fed into Deepdive for marginal inference or weight learning. Under the Deepdive framework, higher precision will be achieved, larger MLNs will be able to be computed and computation will be faster.

### Installation Guide

The Tuffy port code and binary is under the deepdive/mln/ directory. The binary, tuffy.jar, is already compiled and ready to be used. To recompile the source code, you can first make sure ant is installed and type in the following commands:

```bash
ant
ant dist
```

### Usage example

An MLN example is under folder deepdive/examples/smoke_mln. The files in this folder are listed below:

- env_conf.sh is the database configuration file.
- application.conf is the Deepdive program template. Only the Deepdive sampler parameters in this file can be changed.
- prog.mln defines the predicates and formulas of the MLN.
- evidence.db contains the evidence of the MLN.
- query.db contains the queries of the MLN. Check out [Tuffy Documentation](http://i.stanford.edu/hazy/tuffy/doc/) for the format of prog.mln, evidence.db and query.db.
- run.sh controls the workflow of the Deepdive MLN support.

To solve an MLN with Deepdive, the user should 1) configure the database with env_conf.sh and the Deepdive sampler with application.conf, 2) specify the MLN in prog.mln, evidence.db and query.db and 3) start run.sh to solve the MLN.

run.sh will do the following work:

- source env_conf.sh to set database information as environment variables which will be used by Tuffy port and Deepdive.
- Drop and Create the database.
- Generate mln_auto.conf which is the configuration file for Tuffy port.
- Run Tuffy port to populate the database and generate application_auto.conf which is the input for Deepdive.
- Call Deepdive to solved the problem specified in application_auto.conf.

The results, the marginals or weights, will remain in the database and deepdive/out/ after Deepdive finishes.

### Behavior Specification

- The Tuffy port shares exactly the same input format with Tuffy.
- As in Tuffy, soft evidence is supported. A soft evidence will be treated as a factor linked to the atom corresponding to the evidence. The weight of the factor is ln(prior/(1-prior)) where prior is the possibility that the evidence holds true.
- Same as Tuffy, a closed world assumption is made during learning. This means all atoms not specified in evidence.db will be treated as false.
- During learning the system learns weights for all soft rules specified in prog.mln. Non-zero weights are needed to be specified in prog.mln for all soft rules for learning.
- The system puts the results in the database instead of an output file, which is the standard behavior of Deepdive.