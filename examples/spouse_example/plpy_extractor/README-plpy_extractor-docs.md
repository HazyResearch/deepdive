Documentation on plpy_extractor
====

This directory is an example of using plpy_extractors.

These extractors only works on postgresql.

NOTE: Please do learn from the application.conf and udf/pgext_* to learn more about the requirements!



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
- In "run", write your extractor. Return **a list** containing your results, each item in the list should be a list/tuple of your return types.
	- e.g. [[ret1, ret2], [ret1, ret2], ...]

- Do not print. "print" command is not supported in plpy.
- Do not reassign input variables in "run" function! 
	- e.g. "input_var = x" is invalid and will cause error!

The following must be in the same order:
- SQL input query
- order to call "ddext.input" function 
- order of run() argument list

The following must be in the same order:
- order to call "ddext.returns" function
- In actually returned list, order of each tuple
- Order of output_relation (and the column NAMES should match names specified in "ddext.returns")


Caveats / TODOS
----
- Cannot inclide 'AS' in select query, between SELECT and FROM.
    - e.g. you cannot "select p1.id as p1_id"!
    - but you can express most queries without renaming it, and you are able to write "as" after FROM.
    - All you need to do is: to make sure COLUMN ORDER matches EXTRACTOR INPUT ORDER.

- Cannot reassign input variables in "run" function.

