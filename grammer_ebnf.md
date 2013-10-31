## DeepDive Language definition

```
# Program Definition
# ==================================================
program := schema etl statements evidence

# Schema 
# ==================================================
schema := relation_definition*
relation_definition := relation_name relation_schema
relation_name := "\w+"
relation_schema := attribute_varid attribute_definition*
attribute_varid := "varid" "Integer"
attribute_definition := attribute_name attribute_domain
attribute_name := "\w+"
attribute_domain := "Integer" | "String" | "Decimal" | "Float" | "Text" | "Timestamp" | "Boolean" | "Binary"

# ETL
# ==================================================
etl := etl_rule*
etl_rule := etl_soure etl_dest
etl_source := etl_source_file
etl_source_file := "\w+\.(csv|tsv)"
etl_dest := relation_name

# Statements / Extractors
# ==================================================
statements := statement*
statement := relation_name statement_dml statement_udf statement_factor_definition
statement_dml := ? Any SQL SELECT clause that includes the variable ids and optionally a GROUP_BY? 
statement_udf := statement_mapper_script
statement_mapper_script := ? An executable unix script file that adheres to the API ?
statement_factor_definition := factor_name factor_func factor_weight
factor_name := "\w+"
factor_func := "Z = ", factor_func_imply
factor_func_imply := "Imply" factor_var_name*
factor_var_name := "\w+"
factor_weight :=  real_number | "?" [factor_var_name*]

# Evidence
# ==================================================
evidence := evidence_declaration*
evidence_declaration := ? A SQL SELECT clause that selects the evidence ? 
```



