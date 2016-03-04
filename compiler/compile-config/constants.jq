#!/usr/bin/env jq
# constants -- Names to some constant values used across compilation steps
##

def deepdiveVariableIdColumn                :  "id"                             ; # FIXME to dd_id for consistency and safety
def deepdiveVariableLabelColumn             :  "label"                          ; # FIXME to dd_label
def deepdiveGlobalHoldoutTable              :  "dd_graph_variables_holdout"     ;
def deepdiveGlobalObservationTable          :  "dd_graph_variables_observation" ;
def deepdiveGlobalWeightsTable              :  "dd_graph_weights"               ;
def deepdiveReuseWeightsTable               :  "dd_graph_weights_reuse"         ;
def deepdivePrefixForFactorsTable           :  "dd_factors_"                    ;
def deepdivePrefixForWeightsTable           :  "dd_weights_"                    ;
def deepdivePrefixForVariablesCategoriesTable: "dd_categories_"                 ;
def deepdivePrefixForVariablesIdsTable       : "dd_variables_"                  ;
def deepdivePrefixForTemporaryTables         : "dd_tmp_"                        ;
def deepdivePrefixForTemporaryOldTables      : "dd_old_"                        ;
