---
layout: default
---

# DeepDive FAQ


### I get a Java out of memory error, what should I do?

Refer to the [Performance Tuning guide](../advanced/performance.html) and try the following:

- Set the JVM heap size using the `-Xmx` option flag

- If the errors happens while executing an extractor, try decreasing the input
  or output batch size for your extractors. See the [Configuration
  reference](configuration.html#json) for details.

<!-- TODO (Ce) Isn't this only for json_extractors? -->

- If the errors happens during sampling, try setting the JVM heap size for the sampler using the `-Xmx` option flag

<!-- TODO (Ce) Where? Is this still applicable? -->

### How can I debug my extractors?

<!-- TODO (Zifei) Move this to the 'writing extractors' document, together with
the other thing from the walkhtrough (see in the writing extractors document),
add an anchor and just reference it from here. -->

In `json_extractor` (default) and `tsv_extractor`, if you print to *stderr*
instead of *stdout*, the messages will appear in the log file.

In `plpy_extractor`, you should use *plpy.debug* or *plpy.info* to print to console and log file.

### Can I have features for dictionary entries with precise terms (e.g.,
Cretaceous), and less-precise terms (e.g., Recent, Word)?

We suggest that you separate them into two features or inference rules, so that
DeepDive can learn the weight for each dictionary and choose the one to trust.

### If I have two inference rules with the same weight formula, will they share
the same weights?

No, weights are unique within each inference rule. You can force the sharing of
weights by specifying a weight prefix, for example:

```bash
rule1 {
  weight       : ?(relation.someField)
  weightPrefix : "myPrefix"
}
rule2 {
  weight       : ?(relation.someField)
  weightPrefix : "myPrefix"
}
```

<!-- TODO (Ce) What is a weightPrefix and how does it work ? It is not documented. -->


<!-- #### I added an inference rule that I am very confident about, but now my
results are no longer calibrated. What happened?
- - -
This can happen with "fixed" rules that are always true. When you add such a
rule, DeepDive will learn a very large weight for it, which may result in the
inference engine "getting stuck". In such a case, try to lower the learning rate
parameter `-a` (or `--alpha`) for the sampler, for example:

    deepdive {
      sampler.sampler_args: "-l 120 -s 1 -i 200 -a 0.001"
    }

The default value of alpha is set to `0.1`, and during testing it makes sense to
increase or decrease it one order of magnitude at a time. We are actively
working on implementing an adaptive learning rate computation into our sampler
which will help avoid this problem.

<br/>
<br/>

 -->

<!-- TODO (Ce) Why is the above FAQ commented? -->

### I am getting a "java.lang.UnsupportedClassVersionError" error. What can I do?

This happens when you are using an older JRE version not supprted by DeepDive.
Make sure you are using JRE version 1.7.0_45 or greater.


<!-- 
#### I am getting an "ERROR: duplicate key value violates unique constraint "dd_graph_variables_pkey"" error.
- - -
DeepDive automatically assigns unique record IDs through extractor. The above
error happens when record IDs in the database are not unique. This could happen
when you are manually loading data into a table, without using DeepDive's
extractors. If you need to load records manually, make sure their IDs are
globally unique, or define manual sequence to use for your primary keys.
 -->

<!-- TODO (Ce) Why is the above FAQ commented? Remove it if it's no longer
 valid.-->

### I am using Greenplum, and getting an "ERROR: Cannot parallelize an UPDATE
statement that updates the distribution columns"

You should add a `DISTRIBUTED BY` clause in all `CREATE TABLE` commands. Make
sure you distribute your tables in a correct way: do not use the column `id` as
distribution key. Do not use a distribution key that is not initially assigned.


### I am using a plpy_extractor and I get  "ERROR: could not access file
"$libdir/plpython2": No such file or directory"

Make sure your database server supports the PL/python language.

### During sampling, I get an "error while loading shared libraries: libnuma.so.1"

Make sure you have configured the dependencies of the DimmWitted
[sampler](sampler.html) and that you properly set the necessary environmental
variables. Refer to the [Installation guide](installation.html#sampler) for
details.

