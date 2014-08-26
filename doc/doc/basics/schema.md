---
layout: default
---

# Declaring inference variables in the schema

This document describe how to declare random variables on which DeepDive
performs inference and give details about how to use [Categorical/Multinomial
variables](#multinomial).

DeepDive requires the user to specify the name and type of the random variables
on which to perform [inference](../general/inference.html). Currently DeepDive
support Boolean (i.e., Bernoulli) variables and Categorical/Multinomial
variables. Random variables and their types are declared in the
`schema.variables` section of the `application.conf` file. The following is an
example of defining the schema with two Boolean variables:

```bash
deepdive {
  schema.variables {
    people.smokes: Boolean
    people.has_cancer: Boolean
  }
}
```
In the above example `smokes` and `has_cancer` are Boolean attributes in the
`people` table. The name of variables *must* be in the '[table].[column]'
format.

## <a name="multinomial" href="#"></a> Categorical/Multinomial variables

DeepDive supports multinomial variables, which take integer values ranging from
0 to a user-specified upper bound. In order to use a multinomial variable, you
can declare it in the `schema.variables` directive in `application.conf` using
type `Categorical(N)`, to specify that the variable domain is be 0, 1, ..., N-1.
The schema definition would be  look like
  
```bash
schema.variables {
  [table].[column]: Categorical(10)
}
```

The factor function for multinomial is `Multinomial`. It takes multinomial
variables as arguments, and is equivalent to having indicator functions for each
combination of variable assignments. 


For examples, if `a` is a variable taking values 0, 1, 2, and `b` is a variable
taking values 0, 1. Then, `Multinomial(a, b)` is equivalent to the following
factors between a and b
  
    I{a = 0, b = 0}
    I{a = 0, b = 1}
    I{a = 1, b = 0}
    I{a = 1, b = 1}
    I{a = 2, b = 0}
    I{a = 2, b = 1}

Note that each of the factor above has a corresponding weight, i.e., we have one
weight for each possible assignment of variables in the multinomial factor.

### Chunking Example

We include a typical usage example of multinomial variables in the chunking
example under `examples/chunking`. A walkthrough this example, detailing how to
specify Conditional Random Fields and perform Multi-class Logistic Regression is
available [here](chunking.html).
