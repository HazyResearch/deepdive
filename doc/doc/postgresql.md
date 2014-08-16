---
layout: default
---

# Using PostgreSQL with DeepDive

When using [PostgreSQL](postgresql.org) with DeepDive there are several things to pay attention to.

### Preparing the data

DeepDive assumes that you have created the schema for your application in the database. This means, all relations used by any of your extractors and inference rules must exist before running DeepDive. We recommend doing this in a data preparation script, as shown in the [example walkthrough](example.html). When designing your schema, **all relation must have a unique primary key callled `id`**. 

### Connection Information

You define the connection string to your PostgreSQL instance in your application configuration in [JDBC format](http://jdbc.postgresql.org/documentation/80/connect.html). You can optionally specify a username and password for your connection.
    
    deepdive: {
      db.default {
        driver   : "org.postgresql.Driver"
        url      : "jdbc:postgresql://[host]:[port]/[database_name]"
        user     : "deepdive"
        password : ""
        dbname   : [database_name]
        host     : [host]
        port     : [port]

      }
    }

For advanced connection pool options refer to the [Scalikejdbc configuration](http://scalikejdbc.org/documentation/configuration.html).

### Attribute naming conventions in extractors

- Attributes are always prefixed with the name of the relation. For example, if your query includes a `name` column from the `people` table, then the corresponding JSON key would be called `people.name`. This also applies to aliases. For example, `SELECT people.name AS text` would result in a JSON key called `people.text`.
- Aggregates are prepended with a dot and don't have include the relation name. For example `SELECT COUNT(*) AS num FROM people GROUP BY people.name` would result in a JSON key called `.num`.
- When outputting tuples in your extractor, only use the column name of the target relation, without the relation name. In other words, don't have a JSON key called `people.name`, but a key called `name`.


### Attribute naming conventions in inference rules and input queries

- In factor rules, always use `[relation_name].[attribute]` for your variables, regardless whether or not you are using an alias in your input query. For example, even if you write `SELECT p1.is_male from people p1` your variable would be called `people.is_male`.

###  Using self-joins in inference rule input queries

When using self-joins, you must avoid naming collisions by defining an alias in your query. For example, the following will **NOT WORK**:

    SELECT p1.name, p1.id, p2.name, p2.id FROM people p1 LEFT JOIN people p2 ON p1.manager_id = p2.id

Instead, you must write:

    SELECT p1.name AS "p1.name", p1.id AS "p1.id", p2.name AS "p2.name", p2.id AS "p2.id" FROM people p1 LEFT JOIN people p2 ON p1.manager_id = p2.id

And your factor function variables would be called `people.p1.name` and `people.p2.name`.

