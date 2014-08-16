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
     public | dd_graph_weights                             | table    
     public | dd_graph_weights_id_seq                      | sequence 
     public | dd_inference_result_variables                | table    
     public | dd_inference_result_variables_mapped_weights | view     
     public | dd_inference_result_weights                  | table    
     public | f_[RULE_NAME]_query_user                     | view     
	
where `RULE_NAME` is the name of an inference rule.

<!-- TODO (Zifei) we need a one line description for each of the above tables. -->

<!-- TODO (Zifei) Mention the [TABLE]_[VARIABLE]_inference table) -->

