Documentation on plpy_extractor
====

This directory is an example of using plpy_extractors.

These extractors only works on postgresql.

Extractor Config Format (in application.conf )
----
- style: "plpy_extractor"
- input: "[a sql query]"
- output_relation: "[sql table to insert return tuples]"
- udf: "[your script coherent to code format]"  (cannot take parameters)
- after / before / dependency: scripts that maintains function with default extractors


UDF Code Format
----

(see examples: pgext_*.py in udf/ directory)

- Anything out of functions "init", "run" will not be accepted.
- In "init", import libraries, specify input variables and return types
- In "run", write your extractor. Return a list containing your results, each item in the list should be a list/tuple of your return types.
- Do not print. "print" command is not supported in plpy.

- ddext.input: MUST have order
- ddext.returns: MUST have order
- run(argument_list): useless


BUGS / TODOS
----
- Cannot include "." in variable name. (TO BE FIXED)
  - a way to fix: enable "." naming in input, but match input with run arguments..
- Cannot inclide 'AS' in select query (TO BE FIXED)

