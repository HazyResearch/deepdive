---
layout: default
---

# Table names reserved by DeepDive

The following tables are used internally by DeepDive. You cannot create tables with the same name in the database:


                                List of relations
     Schema |                     Name                     |   Type   
    --------+----------------------------------------------+----------
     public | dd_graph_variables_holdout                   | table    
     public | dd_graph_weights                             | table    
     public | dd_graph_weights_id_seq                      | sequence 
     public | dd_inference_result_variables                | table    
     public | dd_inference_result_variables_mapped_weights | view     
     public | dd_inference_result_weights                  | table    
     public | f_[YOUR_RULE_NAME]_query_user                | view     
     
Among these reserved tables, we introduce the most useful two:

## Useful relations

### Examine Factor Weights

View `dd_inference_result_variables_mapped_weights` shows all the learned weights mapped to its descriptions.

          View "public.dd_inference_result_variables_mapped_weights"
        Column     |       Type       | Modifiers | Storage  | Description
    ---------------+------------------+-----------+----------+-------------
     id            | bigint           |           | plain    |
     initial_value | double precision |           | plain    |
     is_fixed      | boolean          |           | plain    |
     description   | text             |           | extended |
     weight        | double precision |           | plain    |


### Examine Variables

Views with name `[TABLE]_[VARIABLE]_inference` (e.g. has_spouse_is_true_inference) contain all variables mapped with their learned expectations. This is useful for examining results for error analysis.

     View "public.has_spouse_is_true_inference"
       Column    |       Type       | Modifiers
    -------------+------------------+-----------
     person1_id  | bigint           |
     person2_id  | bigint           |
     sentence_id | bigint           |
     description | text             |
     is_true     | boolean          |
     relation_id | bigint           |
     id          | bigint           |
     category    | bigint           |
     expectation | double precision |
