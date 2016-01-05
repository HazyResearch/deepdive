#!/usr/bin/env jq-f
# constants -- Names to some constant values used across compilation steps
##

def deepdiveVariableIdColumn                :  "id"                             ;
def deepdiveGlobalHoldoutTable              :  "dd_graph_variables_holdout"     ;
def deepdiveGlobalObservationTable          :  "dd_graph_variables_observation" ;
def deepdiveGlobalWeightsTable              :  "dd_graph_weights"               ;
def deepdivePrefixForFactorsTable           :  "dd_query_"                      ;  # TODO correct prefix to dd_factors_?
def deepdivePrefixForWeightsTable           :  "dd_weights_"                    ;
def deepdivePrefixForMultinomialWeightsTable:  "dd_weightsmulti_"               ;
def deepdivePrefixForVariableCategoriesTable:  "dd_categories_"                 ;
