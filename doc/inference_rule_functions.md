---
layout: default
title: Inference Rule Function Reference
---

# Inference Rule Function Reference

This document lists and describes all functions that can be used in the
`function` directive when [writing inference rules](inference_rules.md).

### Variable Negation
Variables can be negated by prefixing them with a `!`, as in `Or(A,!B)`.

### Imply function

    # B and C and D => A
    Imply(B,C,D,A)

The *Imply* function describes a logical consequence. The last variable is the
head. The variables in the body (`B,C,D`) are combined using a conjunction
(logical AND ([truth
table](http://en.wikipedia.org/wiki/Truth_table#Logical_implication))).

Another version of imply function is *Imply3*, which returns three values:
when the body and the head are true, it returns 1; when the body and the head are false, it returns 0; when the body is true but the head is false, it returns -1.

### Or function

    # A or B or C
    Or(A,B,C)

The *Or* function combines all variables using a logical disjunction ([truth
table](http://en.wikipedia.org/wiki/Truth_table#Logical_disjunction)).

### And function

    # A and B and C
    And(A,B,C)

The *And* function combines all variables using a logical conjunction ([truth
table](http://en.wikipedia.org/wiki/Truth_table#Logical_conjunction)).

### Equal function

    # A <=> B
    Equal(A,B)

The *Equal* function is restricted to two variables and evaluates to true if and
only if both variables have the same value ([truth
table](http://en.wikipedia.org/wiki/Truth_table#Logical_equality)).

### IsTrue function

    IsTrue(A)

The *IsTrue* function evaluates to true if and only if its variable evaluates to
true. It is restricted to one variable. One may implement a *Not* function using
`IsTrue(!A)`.

### Multinomial function

    Multinomial(A,B,C)

The *Multinomial* function allows multinomial variables as arguments, and
has a weight for each variable assignment. This function always evaluates to true.

## DDlog Syntax

### Imply function

    # B and C => A
    B(x), C(x) => A(x) :- ... (conjunctive query body)

### Or function

    # A or B or C
    A(x) v B(x) v C(x) :- ... (conjunctive query body)

### And function

    # A and B and C
    A(x) ^ B(x) ^ C(x) :- ... (conjunctive query body)

### Equal function

    # A = B
    A(x) = B(x) :- ... (conjunctive query body)

### IsTrue function

    A(x) :- ... (conjunctive query body)

### Multinomial function

    Multinomial(A(x)) :- ... (conjunctive query body)
