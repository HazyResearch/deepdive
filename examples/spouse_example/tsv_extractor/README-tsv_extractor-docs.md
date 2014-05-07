Documentation on tsv_extractor
====

This directory is an example of using tsv_extractor.

These extractors only works on postgresql.

NOTE: Please do learn from the application.conf and udf/pgext_* to learn more about the requirements!



Extractor Config Format (in application.conf )
----
- style: "tsv_extractor"
- input: "[a sql query]"
- output_relation: "[sql table to insert return tuples]"
- udf: "[your arbitrary script]"
- after / before / dependency: scripts that maintains function with default extractors


Caveats
----

Input queries are copied into temporary TSV files, split into chunks, and your UDF is executed on them. Your UDF should read in each line as a row in database, whose columns are separated by '\t'.

If you are using array types in input queries, it might be hard to parse in UDF. It's developers duty to make sure the query can be parsed with UDFs. What potentially help is converting these arrays to strings with a special delimiter.

UDFs should output to STDOUT, each line should contain exactly the SAME number of columns with `output_relation`, also separated by '\t'. 
DeepDive will pipe these outputs to files, and an ordinary COPY FROM STDIN command will be executed on the outputs.

