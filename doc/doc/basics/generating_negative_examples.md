---
layout: default
---

## Generating negative evidence

<!-- TODO (Ce/Matteo/Sen) read and improve this written by Zifei, or chat with Zifei to see how he can improve this.
-->

In common relation extraction applications, we often need to generate negative examples in order to train a good system. Without negative examples the system will tend to classify all the variables as positive. However for relation extraction, one cannot easily find enough golden-standard negative examples (ground truth). In this case, we often need to use distant supervision to generate negative examples.

Negative evidence generation can be somewhat an art. The following are several common ways to generate negative evidence, while there might be some other ways undiscussed:

- incompatible relations
- domain-specific knowledge
- random sampling

### Incompatible relations

Incompatible relations are other relations that are always, or usually, conflicting with the relation we want to extract. Say that we have entities `x` and `y`, the relation we want to extract is `A` and one incompatible relation to `A` is `B`, we have:

```
B(x,y) => not A(x,y)
```

As an example, if we want to generate negative examples for "spouse" relation, we can use relations incompatible to spouse, such as parent, children or siblings: If `x` is the parent of `y`,  then `x` and `y` cannot be spouses.

### Domain specific rules

Sometimes we can make use of other domain-specific knowledge to generate negative examples. The design of such rules are largely dependent on the application. 

For example, for spouse relation, one possible domain-specific rule that uses temporal information is that "people that do not live in the same time cannot be spouse". Specifically, if a person `x` has `birth_date` later than `y`'s `death_date`, then `x` and `y` cannot be spouses.

### Random samples

Another way to generate negative evidence is to randomly sample a small proportion among all the variables (people mention pairs in our spouse example), and mark them as negative evidence. 

This is likely to generate some false negative examples, but if statistically variables are much more likely to be false, the random sampling will work.

For example, most people mention pairs in sentences are not spouses, so we can randomly sample a small proportion of mention pairs and mark them as false examples of spouse relation.

<!-- TODO: improve this -->


To see an example of how we generate negative evidence in DeepDive, refer to the [example application walkthrough](walkthrough/walkthrough.html#candidate_relations).

