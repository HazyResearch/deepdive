---
layout: default
---

# Defining Schema

### Defining variable attributes

You must tell DeepDive which variables you want to use in your inference rules. Each variable has a data type associated with it. Currently, DeepDive only supports *Boolean* variables.

{% highlight bash %}
deepdive {
  schema.variables {
    people.smokes: Boolean
    people.has_cancer: Boolean
  }
}
{% endhighlight %}

In the above example *smokes* and *has_cancer* are boolean attributes in the *people* table. DeepDive will assign a unique variable `id` to each entry in the *smokes* and *has_cancer* column. This variable `id` is used during probabilistic inference. Developers should create the variable table such as *people* with the column `id`; if [extractors](extractors.html) output to the variable table, fill `id` column with `NULL` values as extractor output. [See more in the inference guide](inference_rules.html).

### Evidence and Query variables

Evidence is training data that is used to automatically learn [factor weights](inference_rules.html). DeepDive will treat variables with existing values as evidence. In the above example, rows in the *people* table with a `true` or `false` value in the *smokes* or *has_cancer* column will be treated as evidence for that variable. Cells without a value (NULL) value will be treated as query variables.
