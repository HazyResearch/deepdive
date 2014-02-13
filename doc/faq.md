---
layout: default
---

# DeepDive FAQ

<br/>

#### I get a Java out of memory error, what should I do?
---
Refer to the [Performance Tuning guide](/doc/performance.html) and try the following:

- Set the JVM heap size using the `-Xmx` option flag
- If the errors happens while executing an extractor, try decreasing the input or output batch size for your extractors
- If the errors happens during sampling, try setting the JVM heap size for the sampler using the `-Xmx` option flag

### How can I debug extractors?

If you print to *stderr* instead of *stdout* the messages will appear in the log file.

### Can I have features for dictionary entries with precise terms (e.g., Cretaceous), and less-precise terms (e.g., Recent, Word)?

We suggest that you separate them into two features or inference rules. That way DeepDive will learn the weight for each dictionary and choose the one to trust.

### If I have two inference rules with the same weight formula, will they share the same weights?

No, weights are unique within each inference rule. You can force the sharing of weights by speciyfing a weight prefix, for example:

    rule1.weight: ?(relation.someField)
    rule1.weightPrefix: "myPrefix"
    rule2.weight: ?(relation.someField)
    rule2.weightPrefix: "myPrefix"