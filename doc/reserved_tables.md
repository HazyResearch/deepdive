---
layout: default
---

# Table names reserved by DeepDive

The following tables are used internally by DeepDive. You cannot create tables with the same name in the database:

### variables

                                    Table "public.variables"
          Column      |       Type       | Modifiers | Storage  | Stats target | Description 
    ------------------+------------------+-----------+----------+--------------+-------------
     id               | bigint           | not null  | plain    |              | 
     data_type        | text             |           | extended |              | 
     initial_value    | double precision |           | plain    |              | 
     is_evidence      | boolean          |           | plain    |              | 
     is_query         | boolean          |           | plain    |              | 
     mapping_relation | text             |           | extended |              | 
     mapping_column   | text             |           | extended |              | 
     mapping_id       | bigint           |           | plain    |              | 


### Weights

                                        Table "public.weights"
        Column     |       Type       | Modifiers | Storage  | Stats target | Description 
    ---------------+------------------+-----------+----------+--------------+-------------
     id            | bigint           | not null  | plain    |              | 
     initial_value | double precision |           | plain    |              | 
     is_fixed      | boolean          |           | plain    |              | 
     description   | text             |           | extended |              | 


### factor_variables

                         Table "public.factor_variables"
       Column    |  Type   | Modifiers | Storage | Stats target | Description 
    -------------+---------+-----------+---------+--------------+-------------
     factor_id   | bigint  |           | plain   |              | 
     variable_id | bigint  |           | plain   |              | 
     position    | integer |           | plain   |              | 
     is_positive | boolean |           | plain   |              | 


### factors
                                Table "public.factors"
         Column      |  Type  | Modifiers | Storage  | Stats target | Description 
    -----------------+--------+-----------+----------+--------------+-------------
     id              | bigint | not null  | plain    |              | 
     weight_id       | bigint |           | plain    |              | 
     factor_function | text   |           | extended |              | 



### inference_result

                                  Table "public.inference_result"
       Column    |       Type       | Modifiers | Storage | Stats target | Description 
    -------------+------------------+-----------+---------+--------------+-------------
     id          | bigint           | not null  | plain   |              | 
     last_sample | boolean          |           | plain   |              | 
     probability | double precision |           | plain   |              | 



### inference_result_mapped_weights

                     View "public.inference_result_mapped_weights"
        Column     |       Type       | Modifiers | Storage  | Description 
    ---------------+------------------+-----------+----------+-------------
     id            | bigint           |           | plain    | 
     initial_value | double precision |           | plain    | 
     is_fixed      | boolean          |           | plain    | 
     description   | text             |           | extended | 
     weight        | double precision |           | plain    | 




### inference_result_weights

                             Table "public.inference_result_weights"
     Column |       Type       | Modifiers | Storage | Stats target | Description 
    --------+------------------+-----------+---------+--------------+-------------
     id     | bigint           | not null  | plain   |              | 
     weight | double precision |           | plain   |              | 




### mapped_inference_result

                           View "public.mapped_inference_result"
          Column      |       Type       | Modifiers | Storage  | Description 
    ------------------+------------------+-----------+----------+-------------
     id               | bigint           |           | plain    | 
     data_type        | text             |           | extended | 
     initial_value    | double precision |           | plain    | 
     is_evidence      | boolean          |           | plain    | 
     is_query         | boolean          |           | plain    | 
     mapping_relation | text             |           | extended | 
     mapping_column   | text             |           | extended | 
     mapping_id       | bigint           |           | plain    | 
     last_sample      | boolean          |           | plain    | 
     probability      | double precision |           | plain    | 