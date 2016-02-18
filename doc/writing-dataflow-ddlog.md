---
layout: default
title: Defining Data Flow in DDlog
---

# Defining data flow in DDlog

DDlog is a higher-level language for writing DeepDive applications in a succinct, Datalog-like syntax.
Here we focus on describing the general language features for defining the data flow in a DeepDive application.
The DDlog language constructs for [using user-defined functions](writing-udf-python.md) and [specifying a statistical inference model](writing-model-ddlog.md) are described in another page.
A complete end-to-end example written in DDlog is described in the [tutorial](example-spouse.md) in greater detail.

<!--
A reference for the latest language features can be found [here](https://github.com/HazyResearch/ddlog/wiki/DDlog-Language-Features).
-->


## DDlog program

A DDlog program is a collection of declarations and rules.
Each declaration and rule ends with a period (`.`).
Comments in DDlog begin with a hash (`#`) character.
The order of the rules have no meaning.
A DDlog program consists of the following parts:

1. Schema declarations for relations
2. Normal derivation rules
    1. Relation derived (head atom)
    2. Relations used (body atoms)
    3. Conditions
3. User defined functions (UDFs)
    1. Function declarations
    2. Function call rules
4. Inference rules

All DDlog code should be placed in a file named [`app.ddlog` under the DeepDive application](deepdiveapp.md#app-ddlog).


## Schema declarations

First of all, the schema of the *relations* defined and used throughout the program should be declared.
These relations are mapped to tables in the database configured for the DeepDive application.
The [documentation for managing the database](ops-data.md) introduces various ways provided by DeepDive to interact with it.
The order of declarations doesn't matter, but it's a good idea to place them at the beginning because that makes it easier to understand the rest of the rules that refer to the relations.

```ddlog
relation_name(
  column1_name  column1_type,
  column2_name  column2_type,
  ...
).
```
Similar to a SQL table definition, a schema declaration is just the name of a relation followed by a comma separated list of the column names and their types.
Currently, DDlog maps the types directly to SQL, so any type supported by the underlying database can be used, e.g., [PostgreSQL's types](http://www.postgresql.org/docs/9.1/static/datatype.html#DATATYPE-TABLE).

Below is a realistic example.

```ddlog
article(
  id     int,
  length int,
  author text,
  words  text[]
).
```
Here we are defining a relation named "article" with four columns, named "id", "length", "author" and "words" respectively.
Each column has it's own type, here utilizing `int`, `text` and `text[]`.


## Normal derivation rules

Typical Datalog-like rules are used for defining how a relation is derived from other relations.
For example, the following rule states that the tuples of relation `Q` are derived from relations `R` and `S`.

```ddlog
Q(x, y) :- R(x, y), S(y).
```

Here, `Q(x, y)` is the *head atom*, and `R(x, y)` and `S(y)` are *body atoms*.
`x` and `y` are *variables* in the rule.
The head is the relation being defined, and the body is a conjunction of predicates or conditions that bound the variables used for the definition, separated by commas (`,`).
In this example, the second column of `R` is *unified* with the first column of `S`, i.e., the body is an *equi-join* between relations `R` and `S`.

### Expressions
In the atom columns, both variables and expressions can be used.
DDlog's semantics is based on unification of variables and expressions, i.e., variables with the same name unify the columns, and expressions in the body unify the values with the corresponding columns.
More concretely, expressions in the head atom decide values for the columns of the head relation, while expressions in the body atom constrain the values of the body columns.

#### Examples
A few examples of expressions are shown below.

```ddlog
a(k int).
b(k int, p text, q text, r int).
c(s text, n int, t text).

Q("test", f(123), id) :- a(id), b(id, x,y,z), c(x || y,10,"foo").

R(TRUE, power(abs(x-z), 2)) :- a(x), b(y, _, _, _).

S(if l > 10 then TRUE
else if l < 10 then FALSE
else NULL end) :- R ( _, l).
```

Here tuples of string literal "text", a function `f` applied to 123, and `id` bound by the body of the rule are added to relation `Q`. Then, we add to the relation `R` the boolean `TRUE` and operations from variables in `a` and `b`. Finally, we fill out the relation `S` by a condition value according to the second variable in `R`.

We observe that sophisticated queries are realizable in ddlog. The following paragraphes explain in details the different possible operations in ddlog.

<!--
The repeated use of variables in the body expresses the following conditions expressed in SQL:

```
    a.k = b.k         -- unifying variable id used in the first columns of a and b
AND c.n = 10          -- unifying literal 10 with the second column of c
AND c.t = 'foo'       -- unifying literal "foo" with the third column of c
AND b.p || b.q = c.s  -- unifying expression x||y with the first column of c
```
-->

#### Constant literals
Numbers, text strings, boolean values, null value are expressed as follows:

* Numeric literals, e.g.: `123`, `-456.78`.

* String literals are surrounded by double quotes (`"`), e.g.: `"Hello, World!"`.

    Preceding backslash (`\`) is used as the escape sequence for including double quote itself as well as backslash:

    ```
    "For example, \"There're backslashes (\\) before the surrounding double quotes.\""
    ```

    is how the following text is written as string literal:

    ```
    For example, "There're backslashes (\) before the surrounding double quotes."
    ```

* Boolean literals: `TRUE`, `FALSE`.

* Null literal: `NULL`.

#### Operators
Derived values are expressed as <code>*expr* *op* *expr*</code> or <code>*uop* *expr*</code> where *op* is a binary operator and *uop* is a unary operator among the following ones:

Binary operators

* Numerical arithmetic `+`, `-`, `*`, `/`
* String concatenation `||`

Unary operators

* Negative sign `-`
* Negation of boolean values `!`

Expressions can be arbitrarily nested, and parentheses can surround them <code>(*expr*)</code> to make the operator association clear.

#### Comparisons

* Equality `=`
* Distinction `!=`
* Inequality `<`, `<=`, `>`, `>=`
* Null <code>*expr* IS NULL</code>, <code>*expr* IS NOT NULL</code>
* String pattern <code>*exprText* LIKE *exprPattern*</code>

#### Conditional values
It is possible to express values determined by conditional cases with a syntax of <code>if *expr* then *expr* else *expr* end</code>.
For example, the following expresses a value 1 when variable `x` is positive, -1 when it's negative, otherwise `NULL`.

```ddlog
if x > 0 then 1
else if x < 0 then -1
else NULL
end
```

#### Function applications
More complex values can be expressed as applying predefined functions to a tuple of expressions with the following syntax: <code>*name*(*expr*, *expr*, ...)</code>.
Currently, DDlog directly maps the function *name* directly to SQL, so any function defined by the underlying database can be used, e.g., [PostgreSQL's functions](http://www.postgresql.org/docs/9.1/static/functions.html).
For example, the following takes the absolute value using the `abs` function and the `power` function to square the value.

```ddlog
power(abs(x - y), 2)
```

#### Type casts
When a value needs to be turn into a different type, e.g., integer into a floating point, or vice versa, then a type casting syntax can follow the expression, i.e., <code>*expr* :: *type*</code>.
For example, the following turns the variable in the denominator into a floating point number, so the ratio can be correctly computed:

```ddlog
num_occurs / total :: FLOAT
```


### Placeholder
A placeholder `_` can be used in the columns of body atoms, indicating no variable is relevant to that column.
This is useful if only a subset of columns are needed for the derivation.
For example, the following rule uses only two columns of `R` to define `Q`, and the rest of the columns are not bound to any variable:

```ddlog
Q(x, y, x + y) :- R(x, _, _, y, _, _, _).
```

<!-- TODO contrast patterns (in bodies) from expressions (in head) -->

### Conditions
Conditions are put at the same level of body atoms, and collectively with body atoms bound the variables used in the head.
Conjunctive conditions are separated by commas (`,`), and disjunctive conditions are separated by semicolon (`;`).
Exclamation (`!`) can precede a condition to negate it.
Conditions can be arbitrarily nested by putting inside square brackets (<code>[*cond*, *cond*, ...; *cond*; ...]</code>).
For example, the following rule takes the values for variable `x` from the first column of relation `b` that satisfy a certain condition with the fourth column of `b`.

```ddlog
Q(x) :- b(x,_,_,w), [x + w = 100; [!x > 50, w < 10]].
```

### Disjunctions
Disjunctive cases can be expressed as multiple rules defining the same head, or as one rule having multiple bodies separated by semicolons (`;`).
Take the following two rules as an example.

```ddlog
Q(x, y) :- R(x, y), S(y).
Q(x, y) :- T(x, y).
```

They can be written as a single rule:

```ddlog
Q(x, y) :- R(x, y), S(y); T(x, y).
```

### Aggregation
Instead of deriving one head tuple for every combination of variables satisfying the body, a group of head tuples can be reduced into one by taking aggregate values for some of the columns.
By surrounding some columns of the head with an aggregate function, the aggregates are computed from each group of column values determined by the values of other columns not under aggregation.
DDlog recognizes the following aggregate functions:

* `SUM`
* `MAX`
* `MIN`
* `ARRAY_AGG`
* `COUNT`

For example, the following rule groups all values of the third column of `R` by the first and second columns, then take the maximum value for deriving a tuple for `Q`.

```ddlog
Q(a,b,MAX(c)) :- R(a,b,c).
```

### Select distinct

In order to select only distinct elements from other relations, the operator `*:-` can be used. For instance, let's consider the following rule:

```ddlog
Q(x,y) *:- R(x, y).
```

In this rule, only disctinct couple of variables `(x,y)` from the relation `R` will be inserted in the head `Q`.


### Quantifiers

#### Existential/Universal
Another way to derive a reduced number of tuples for the head is to use *quantified bodies*.
Body atoms and conditions can be put under *quantifiers* to express existential (`EXISTS`) or universal (`ALL`) constraints over certain variables with the following syntax:

```ddlog
body, ..., EXISTS[body, body, ...], body, ...
body, ...,    ALL[body, body, ...], body, ...
```

For the variables used both inside and outside of the quantification, the relations inside the quantifier express constraints placed on the variables already bound by some relation outside the quantifier.
`EXISTS` expresses the constraint for such variables that there must exist a combination of values satisfying the quantified body, while `ALL` means for each combination of values for the variables all possible bindings for other variables within the quantified body must be satisfied.
This makes it possible to express complex conditions using derived values in relations.
Here are two example rules using quantifiers:

```ddlog
P(a) :- R(a, _), EXISTS[S(a, _)].
Q(a) :- R(a, b),    ALL[S(a, c), c > b].
```

The first rule means `P` contains all the first columns of relation `R` that also appears at least once in the first column of relation `S`.
The second rule means `Q` contains all the first columns of relation `R` if tuples of relation `S` with the same value on the first column always have a second column that is greater than the value for the second column of `R`.


#### Optional
`OPTIONAL` quantifier can be used when certain body atoms are optional.
It means that a tuple for the head relation should still be derived even if there may not be tuples in certain body relations.
The variables appear only in the optional bodies are bound to `NULL` when they cannot be satisfied.
This quantifier roughly corresponds to *outer joins* in SQL.

Here is an example rule using an optional quantifier:

```ddlog
Q(a, c) :- R(a, b), OPTIONAL[S(a, c), c > b].
```
It means `Q` contains every value of the first column of relation `R` paired with all values for the second column of relation `S` tuples that have the same value on the first column and the second column is greater than that of `R`, or `NULL` if no such value exists.
