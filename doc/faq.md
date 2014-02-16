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

<br/>
<br/>

#### How can I debug extractors?
---
If you print to *stderr* instead of *stdout* the messages will appear in the log file.

<br/>
<br/>

#### Can I have features for dictionary entries with precise terms (e.g., Cretaceous), and less-precise terms (e.g., Recent, Word)?
---
We suggest that you separate them into two features or inference rules. That way DeepDive will learn the weight for each dictionary and choose the one to trust.

<br/>
<br/>

#### If I have two inference rules with the same weight formula, will they share the same weights?
---
No, weights are unique within each inference rule. You can force the sharing of weights by speciyfing a weight prefix, for example:

    rule1.weight: ?(relation.someField)
    rule1.weightPrefix: "myPrefix"
    rule2.weight: ?(relation.someField)
    rule2.weightPrefix: "myPrefix"


<br/>
<br/>

#### I added an inference rule that I am very confident about, but now my results are no longer calibrated. What happened?
---
This can happen with "fixed" rules that are always true. When you add such a rule, DeepDive will learn a very large weight for it, which may result in the inference engine "getting stuck". In such a case, try to lower the learning rate parameter `--alpha` for the sampler, for example:

    deepdive.sampler.sampler_args: "-l 120 -s 1 -i 200 --alpha 0.001"

The default value of alpha is set to `0.1`, and during testing it makes sense to increase or decrease it one order of magnitude at a time. We are actively workign on implementing an adaptive learning rate computation into our sampler which will help avoid this problem.





