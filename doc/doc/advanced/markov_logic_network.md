---
layout: default
---

# Markov Logic Network

This page introduces Deepdive's support for [Markov Logic Network](http://en.wikipedia.org/wiki/Markov_logic_network)(MLN). This piece of work takes the program parser of [Tuffy](http://i.stanford.edu/hazy/hazy/tuffy/) to generate a database containing all atoms in the MLN and a Deepdive program containing the formulas in the MLN as Deepdive [inference rules](http://deepdive.stanford.edu/doc/basics/inference_rules.html). Then the generated Deepdive program will be fed into Deepdive for [marginal inference or weight learning](http://deepdive.stanford.edu/doc/general/inference.html). Under the Deepdive framework, higher precision will be achieved, larger MLNs will be able to be computed and computation will be faster.

### Installation Guide

The Tuffy port code and binary is under the deepdive/mln/ directory. The binary, tuffy.jar, is already compiled and ready to be used. To recompile the source code, you can first make sure ant is installed and type in the following commands:

```bash
ant
ant dist
```

### Usage example

MLN examples can be found under folders deepdive/examples/*_mln. The files in each of these folders are listed below:

- env_conf.sh is the database configuration file.
- [application.conf](http://deepdive.stanford.edu/doc/basics/configuration.html) is the Deepdive program template. Only the Deepdive sampler parameters and the global parameters except the database settings in this file can be changed.
- prog.mln defines the predicates and formulas of the MLN.
- evidence.db contains the evidence of the MLN.
- query.db contains the queries of the MLN. Check out [Tuffy Documentation](http://i.stanford.edu/hazy/tuffy/doc/) for the format of prog.mln, evidence.db and query.db. You can find more examples in deepdive/mln/examples/.
- run.sh controls the workflow of the Deepdive MLN support.

To solve an MLN with Deepdive, the user should 1) configure the database with env_conf.sh and the Deepdive sampler with application.conf, 2) specify the MLN in prog.mln, evidence.db and query.db and 3) start run.sh to solve the MLN.

run.sh will do the following work:

- source env_conf.sh to set database information as environment variables which will be used by Tuffy port and Deepdive.
- Drop and Create the database.
- Generate mln_auto.conf which is the configuration file for Tuffy port.
- Run Tuffy port to populate the database and generate application_auto.conf which is the input for Deepdive.
- Call Deepdive to solved the problem specified in application_auto.conf.

The results, the marginals or weights, will remain in the [database](http://deepdive.stanford.edu/doc/advanced/reserved_tables.html) and deepdive/out/ after Deepdive finishes.

### Behavior Specification

- The Tuffy port shares exactly the same input format with Tuffy.
- As in Tuffy, soft evidence is supported. A soft evidence will be treated as a factor linked to the atom corresponding to the evidence. The weight of the factor is ln(prior/(1-prior)) where prior is the possibility that the evidence holds true.
- Same as Tuffy, a closed world assumption is made during learning. This means all atoms not specified in evidence.db will be treated as false.
- During learning the system learns weights for all soft rules specified in prog.mln. Non-zero weights are needed to be specified in prog.mln for all soft rules for learning.
- For now the Tuffy Port is only tested on Postgres. Its support for other DBMSs will be developed and tested in the future. The users should configure the DBMS settings only in env_conf.sh.
