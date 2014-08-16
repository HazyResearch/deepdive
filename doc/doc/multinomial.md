---
layout: default
---

# Using Multinomial

DeepDive supports multinomial variables, which take integer values ranging from 0 to an upper bound. To use a multinomial type variable, you can declare the variable `schema.variables` in `application.conf` using type `Categorical(N)`, where the variable domain will be 0, 1, ..., N-1. The schema definition would be like this 
  
    schema.variables {
      [table].[column]: Categorical(10)
    }

The factor function for multinomial is `Multinomial`. It takes multinomial variables as arguments, and is equivalent to having indicator functions for each combination of variable assignments. For examples, if `a` is a variable taking values 0, 1, 2, and `b` is a variable taking values 0, 1. Then, `Multinomial(a, b)` is equivalent to the following factors between a and b
  
    I{a = 0, b = 0}
    I{a = 0, b = 1}
    I{a = 1, b = 0}
    I{a = 1, b = 1}
    I{a = 2, b = 0}
    I{a = 2, b = 1}

Note each of the factor above has a corresponding weight, i.e., we have the one weight for each possible assignments of variables in multinomial factor.

# Chunking Example

We include a chunking example under `examples/chunking` to illustrate how to use multinomial. The documentation can be found [here](chunking.md). In the example, we include typical usage of multinomial, which is also described below. 

## Multi-class Logistic Regression

Multi-class classification problem can be compactly represented in DeepDive. For each variable to predict, add `Multinomial` factors with weight depending on the feature. For example, if we have a variable table `var`, which has the variable to predict in colomn `value`. We also have a feature table `features` which contains features for the variables. A typical logistic regression inference rule will look like
  
    factor_lr {
      input query: """SELECT var.id AS "var.id", var.value AS "var.value", features.feature as "feature"
        FROM var, features
        WHERE predicate
      function: "Multinomial(var.value)"
      weight: "?(feature)"
    }


## Conditional Random Field

To express conditional random field, just write `Multinomial` factor for variables you want to link. For example, in the chunking example, if you want to link neiboring words, you can write the following

    factor_linear_chain_crf {
      input_query: """select w1.id as "words.w1.id", w2.id as "words.w2.id", w1.tag as "words.w1.tag", w2.tag as "words.w2.tag"
        from words w1, words w2
        where w2.word_id = w1.word_id + 1"""
      function: "Multinomial(words.w1.tag, words.w2.tag)"
      weight: "?"
    }

where words is the table that contains the words, where the tag column is the chunking label to predict. It is similar with skip-chain CRF, where we have skip edges that link identical words. In chunking example, we have

    factor_skip_chain_crf {
      input_query: """select *
      from
        (select w1.id as "words.w1.id", w2.id as "words.w2.id", w1.tag as "words.w1.tag", w2.tag as "words.w2.tag",
          row_number() over (partition by w1.id) as rn
        from words w1, words w2
        where w1.tag is not null and w1.sent_id = w2.sent_id and w1.word = w2.word and w1.id < w2.id) scrf
      where scrf.rn = 1""" 
      function: "Multinomial(words.w1.tag, words.w2.tag)"
      weight: "?"
    }

where we do a self join on words table to get the link between identical words, and then prune the results to select only one link for each word.