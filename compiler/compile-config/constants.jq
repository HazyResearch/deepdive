#!/usr/bin/env jq
# constants -- Names to some constant values used across compilation steps
##

def deepdiveVariableIdColumn                :  "dd_id"                          ;
def deepdiveVariableLabelColumn             :  "dd_label"                       ;
def deepdiveVariableLabelTruthinessColumn   :  "dd_truthiness"                  ;
def deepdiveVariableExpectationColumn       :  "expectation"                    ;
def deepdiveVariableInternalLabelColumn     :  "dd__label"                      ;
def deepdiveVariableInternalFrequencyColumn :  "dd__count"                      ;
def deepdiveGlobalHoldoutTable              :  "dd_graph_variables_holdout"     ;
def deepdiveGlobalObservationTable          :  "dd_graph_variables_observation" ;
def deepdiveGlobalWeightsTable              :  "dd_graph_weights"               ;
def deepdiveInferenceResultWeightsTable     :  "dd_inference_result_weights"    ;
def deepdiveInferenceResultWeightsMappingView: "dd_inference_result_weights_mapping";
def deepdiveInferenceResultVariablesTable   :  "dd_inference_result_variables"  ;
def deepdiveReuseWeightsTable               :  "dd_graph_weights_reuse"         ;
def deepdivePrefixForFactorsTable           :  "dd_factors_"                    ;
def deepdivePrefixForWeightsTable           :  "dd_weights_"                    ;
def deepdivePrefixForWeightGroupsTable      :  "dd_weightgroups_"               ;
def deepdivePrefixForVariablesCategoriesTable: "dd_categories_"                 ;
def deepdivePrefixForVariablesIdsTable       : "dd_variables_"                  ;
def deepdivePrefixForTemporaryTables         : "dd_tmp_"                        ;
def deepdivePrefixForTemporaryOldTables      : "dd_old_"                        ;
