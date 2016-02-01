---
layout: default
title: Compiling a DeepDive application
---

<todo>
TO ADD:

* More info about the "why" it compiles.
* check that app.ddlog has been correcly introduced before.
* check that the fact that input data must be located in input/ (with symlinks or not) is previously defined. In particular, precise the link in the "DeepDive commands" section.
* proof read the "Interesting compiled files" section.
* maybe split "intersting compiled files" and a "where are compiled files stored?"
* run/Makefile ?

</todo>

# Compiling a DeepDive application

This document details how a DeepDive application is compiled. In particular, we describe here the use of the different `deepdive` commands, why and where it compiles and which files can be interesting to look at.

- [Why compiling ?](#why_compiling)
- [DeepDive compile](#deepdive_compile)
- [DeepDive check](#deepdive_check)
- [Interesting compiled files](#compilation_where)
- [When to compile ?](#compilation_when)


## <a name="why_compiling" href="#"></a> Why compiling ?

While creating a Deepdive application, the main flow of the application is detailed in the `app.ddlog` file. However, this file has to be compiled before being executed by DeepDive.

Compiling a DeepDive application presents two main advantages. First it helps to run the application faster by extracting the correct dependencies between the different operations. Second, it provides many checks for the application and helps detecting errors even before running DeepDive.

## <a name="deepdive_compile" href="#"></a> DeepDive compile

To compile a DeepDive application, simply run `deepdive compile` in the main DeepDive folder. This will compile the `app.ddlog`, adding commands from a `deepdive.conf` file if any. Let's observe that it may be useful to add the folder run/ in .gitignore if you share your DeepDive application on github.

## <a name="deepdive_check" href="#"></a> DeepDive check

All the extractors and tables declarations can be written in any order in `app.ddlog`: the whole dataflow is extracted during the compilation and many checks are made. All these checks can also be performed individually by the command `deepdive check`. In particular, the following checks are made:

- ```input_extractors_well_defined```: checks sanity of all defined extractors.
- ```input_schema_wellformed```: checks if schema variables are well formed.
- ```compiled_base_relations_have_input_data```: all the tables declared that are not filled by an extractor must have input data in input/ as explained here. (\#TODO: ADD LINK)
- ```compiled_dependencies_correct```: check that the dependencies are correct. In particular, in the application includes extractors in deepdive.conf, each output relation must be output by exactly one extractor.
- ```compiled_input_output_well_defined```: checks if all inputs and outputs of compiled processes are well-defined.
- ```compiled_output_uniquely_defined```: checks if all outputs in the compiled documents are defined by one process.

## <a name="compilation_where" href="#"></a> Interesting compiled files

All the compiled documents are stored in the folder `run/`. Each compilation creates the config files in a new folder in `run/`, named as the time when the compilation was made. In particular, the latest compiled version can be found in `run/LATEST.COMPILE/`. For instance, the compiled deepdive.conf can then be found at `run/LATEST.COMPILE/deepdive.conf`.

Most of the processes are stored in folders under run/. In particular, run/data/, run/model/ and run/process/**/run.sh contain the different scripts that will be run by deepdive after a certain `deepdive do` command.

Moreover, a `run/dataflow.svg` is created. This file contains a graph of the dataflow and helps understanding better the dependencies between tables and extractors. It is to be opened in a browser (Chrome seems to work particularly well for it).


## <a name="compilation_when" href="#"></a> When to compile ?

Before an execution of `deepdive do`, if `app.ddlog` has been modified (or if `deepdive.conf`, if any, has been modified), `deepdive compile` should be executed. Otherwise, the modifications in app.ddlog won't be considered by DeepDive.

