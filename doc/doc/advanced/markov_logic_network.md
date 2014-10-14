---
layout: default
---

# Markov Logic Networks

This page introduces Deepdive's support for [Markov Logic
Networks](http://en.wikipedia.org/wiki/Markov_logic_network) (MLNs). This piece of
work takes the program parser of [Tuffy](http://i.stanford.edu/hazy/hazy/tuffy/)
to generate a database containing all atoms in the MLN and a Deepdive program
containing the formulas in the MLN as Deepdive [inference
rules](../basics/inference_rules.html). The generated Deepdive program will be
then fed into Deepdive for [marginal inference or weight
learning](../general/inference.html). Deepdive is able to achieve higher
precision, handle larger MLNs, and perform the computation faster than the
original Tuffy system.


### Installation Guide

The Tuffy port code and binary is under the `deepdive/mln/` directory. The binary,
`tuffy.jar`, is already compiled and ready to be used. To recompile the source
code, you must first make sure that `ant` is installed and type in the following
commands:

```bash
ant
ant dist
```

### Usage example

MLN examples can be found in directories with name ending in `_mln` under
`DEEPDIVE_HOME/examples/` (assuming `DEEPDIVE_HOME` is the DeepDive installation
directory). Each directory contains the following files:

- `env_conf.sh` is the database configuration file.
- [application.conf](../basics/configuration.html) is the Deepdive program
	template. Only the Deepdive sampler parameters and the global parameters
	except the database settings in this file can be changed.
- `prog.mln` defines the predicates and formulas of the MLN.
- `evidence.db` contains the evidence of the MLN.
- `query.db` contains the queries of the MLN. Check out the [Tuffy
	Documentation](http://i.stanford.edu/hazy/tuffy/doc/) for the format of
	`prog.mln`, `evidence.db` and `query.db`. You can find more examples under
	`DEEPDIVE_HOME/mln/examples/`.
- `run.sh` controls the workflow of the Deepdive MLN support.

To solve an MLN with Deepdive, the user should 

1) configure the database with `env_conf.sh` and the Deepdive sampler with
`application.conf`

2) specify the MLN in `prog.mln`, `evidence.db`, and `query.db` 

3) start run.sh to solve the MLN.

The `run.sh` script will do the following work:

- source `env_conf.sh` to set database information as environment variables
	which will be used by Tuffy port and Deepdive.
- Drop and create the database.
- Generate `mln_auto.conf` which is the configuration file for the Tuffy port.
- Run the Tuffy port to populate the database and generate
	`application_auto.conf` which is the input configuration file for Deepdive.
- Call Deepdive to solve the problem specified in `application_auto.conf`.

The results, the marginals or weights, are stored in the
[database](reserved_tables.html) and in `DEEPDIVE_HOME/out/` once the execuption
of Deepdive is complete.

### Behavior Specification

- The Tuffy port shares exactly the same input format with Tuffy.
- As in Tuffy, soft evidence is supported. A soft evidence will be treated as a
	factor linked to the atom corresponding to the evidence. The weight of the
	factor is ln(prior/(1-prior)) where prior is the possibility that the
	evidence holds true.
- Like in Tuffy, a closed world assumption is made during learning. This means
	all atoms not specified in `evidence.db` will be treated as false.
- During the learning phase the system learns weights for all soft rules
	specified in `prog.mln`. Non-zero weights are needed to be specified in
	`prog.mln` for all soft rules for learning.
- For now the Tuffy Port is only tested on Postgres. Its support for other DBMSs
	will be developed and tested in the future. The users should configure the
	DBMS settings *only* in `env_conf.sh`.
