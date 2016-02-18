#!/usr/bin/env jq
# constants -- Names to some constant values used across compilation steps
##

def deepdiveVariableIdColumn                :  "id"                             ;
def deepdiveGlobalHoldoutTable              :  "dd_graph_variables_holdout"     ;
def deepdiveGlobalObservationTable          :  "dd_graph_variables_observation" ;
def deepdiveGlobalWeightsTable              :  "dd_graph_weights"               ;
def deepdiveReuseWeightsTable               :  "dd_graph_weights_reuse"         ;
def deepdivePrefixForFactorsTable           :  "dd_factors_"                    ;
def deepdivePrefixForWeightsTable           :  "dd_weights_"                    ;
def deepdivePrefixForMultinomialWeightsTable:  "dd_weightsmulti_"               ;
def deepdivePrefixForVariableCategoriesTable:  "dd_categories_"                 ;
