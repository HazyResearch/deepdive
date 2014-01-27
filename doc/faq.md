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