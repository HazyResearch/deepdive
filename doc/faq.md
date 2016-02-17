---
layout: default
title: FAQ
---

# DeepDive FAQ

#### `deepdive do` keeps canceling when I close my editor.  How can I make it actually run anything?

You need to **explicitly write/save the plan file** presented in your editor to have DeepDive proceed with [its execution](ops-execution.md).
(Think about Git commit messages shown in your editor if that makes sense.)

To prevent it from keep asking you to confirm through your editor, you can set the following variable in your environment.

```bash
export DEEPDIVE_PLAN_EDIT=false
```

Then, DeepDive never asks for confirmation before actually executing the plan.


#### DDlog is giving me obscure errors.  How can I see more details?

Recompile your app after running the following to enable stack trace in the DDlog compiler:

```bash
export DDLOG_STACK_TRACE=1
```

[Reporting an issue](https://github.com/HazyResearch/deepdive/issues/new) with such information helps us a lot to debug your problem.


#### How can I debug my extractors?

See [debugging UDFs](debugging-udf.md) page.


#### Can I have features for dictionary entries with precise terms (e.g., Cretaceous), and less-precise terms (e.g., Recent, Word)?

We suggest that you separate them into two features or inference rules, so that
DeepDive can learn the weight for each dictionary and choose the one to trust.


#### If I have two inference rules with the same weight formula, will they share the same weights?

No, weights are unique within each inference rule, this is achieved in DeepDive by concatenating a ''prefix'' to the feature, which by default is the name of the corresponding rule.
You can force the sharing of weights by specifying this prefix, for example:

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

<todo>translate into DDlog</todo>


#### I am getting a `java.lang.UnsupportedClassVersionError` error. What can I do?

This happens when you are using an older Java (JRE) version not supported by DeepDive.
Make sure you are using Java 8 or later.


#### I am using Greenplum, and getting an `ERROR: Cannot parallelize an UPDATE statement that updates the distribution columns`

You should add a `@distributed_by` annotation to the columns in your relations in app.ddlog.
(Which meant `DISTRIBUTED BY` clause in all `CREATE TABLE` SQL statements in the old days.)
Make sure you distribute your tables in a correct way: do not use the column `id` as distribution key.
Do not use a distribution key that is not initially assigned.


#### I get `ERROR: could not access file "$libdir/plpython2": No such file or directory`

Make sure your database supports the [PL/Python language extension](http://www.postgresql.org/docs/current/interactive/plpython.html).

