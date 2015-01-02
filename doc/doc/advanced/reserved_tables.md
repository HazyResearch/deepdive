---
layout: default
---

# Internal database schema

The following tables are used internally by DeepDive. There can *not* be tables
with the same name in the database:


                                List of relations
     Schema |                     Name                     |   Type   
    --------+----------------------------------------------+----------
     public | dd_graph_variables_holdout                   | table    
     public | dd_graph_variables_observation               | table    
     public | dd_graph_weights                             | table    
     public | dd_graph_weights_id_seq                      | sequence 
     public | dd_inference_result_variables                | table    
     public | dd_inference_result_weights                  | table    
     public | dd_inference_result_weights_mapping          | view     
     public | dd_feature_statistics_support                | table
     public | dd_feature_statistics                        | view
     public | dd_query_[RULE_NAME]                         | table
     public | dd_weights_[RULE_NAME]                       | table
     public | [TABLE]_[VARIABLE]_inference                 | view
     public | [TABLE]_[VARIABLE]_inference_bucketed        | view
     public | [TABLE]_[VARIABLE]_calibration               | view
     public | [TABLE]_[VARIABLE]_cardinality               | table
	
where `[RULE_NAME]` is the name of an inference rule, `[TABLE]` is the
name of a table that contains variables, and `[VARIABLE]` is the name
of a variable in the corresponding table.

Description of each schema:

- `dd_graph_variables_holdout`: a table that contains all variable ids that are used for holdout. Can be used for custom holdout by a [holdout query](../basics/calibration.html#custom_holdout).

- `dd_graph_variables_observation`: a table that contains all variable ids that are evidence that will not be fitted during learning. An usage example of this table can be found [here](../basics/configuration.html#calibration).

- `dd_graph_weights`: a table that contains all the materialized weights.

- `dd_graph_weights_id_seq`: a sequence used in creating weights.

- `dd_inference_result_variables`: a table that contains the inference results (expectation) for all query variables.

- `dd_inference_result_weights`: a table that shows factor weight ids and learned weight values.

- `dd_inference_result_weights_mapping`: a view that maps all distinct factor weights to their description and  their learned values. It is a commonly used view that shows the learned weight value of a factor as well as the number of occurences of a factor.

- `dd_feature_statistics_support`: a table that maps all distinct feature descriptions with number of positive, negative examples and query variables. 
    - e.g. feature "word_seq=[is-married-to]" is associated with 1000 positive examples, 10 negative examples, and has 3000 query variables.
    - The statistics are only for unary factors (with factor function `IsTrue`).

- `dd_feature_statistics`: a view that joins `dd_inference_result_weights_mapping` and `dd_feature_statistics_support`. It gathers all distinct feature descriptions, weights, and number of positive, negative examples and query variables. 
    - Non-unary factors will have NULL values in the statistics columns.

- `dd_query_[RULE_NAME]`: a table that is defined by the input query of an [inference rule](../basics/inference_rules.html). You can use it as a feature table in BrainDump.

- `dd_weight_[RULE_NAME]`: a table that stores initial weights for factors, used internally.

- `[TABLE]_[VARIABLE]_inference`: a view that maps variables with their inference results. It is commonly used for error analysis.

- `[TABLE]_[VARIABLE]_inference_bucketed`: a table that maps variables with their inference results, with expectations separated into 10 buckets. It is used for generating calibration plots.

- `[TABLE]_[VARIABLE]_calibration`: a view that has calibration statistics of a variable. Used in generating calibration plots.

- `[TABLE]_[VARIABLE]_cardinality`: a table that records the cardinality of given variable. For example if the domain of a variable is {1,2,3} the cardinality is 3. Note, this is used by grounding.

